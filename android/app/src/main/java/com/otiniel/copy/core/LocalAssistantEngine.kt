package com.otiniel.copy.core

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.ThinkingConfig
import com.otiniel.copy.data.ConversationMessage
import com.otiniel.copy.data.LocalModelRepository
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Runs a user-imported LiteRT-LM model on-device, without an external inference API.
 * One private background executor owns engine and conversation native resources.
 */
class LocalAssistantEngine(
    context: Context,
    private val modelRepository: LocalModelRepository,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val cacheDirectory = appContext.cacheDir.absolutePath
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "Copy-Local-AI").apply { isDaemon = true }
    }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val cleanupScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutex = Mutex()
    private val closeRequested = AtomicBoolean(false)

    private var engine: Engine? = null
    private var activeConversation: Conversation? = null
    private var loadedModelKey: String? = null

    suspend fun generate(
        prompt: String,
        savedMemoryContext: String,
        history: List<ConversationMessage>,
    ): String = withContext(dispatcher) {
        check(!closeRequested.get()) { "O mecanismo de IA local está sendo encerrado." }
        mutex.withLock {
            check(!closeRequested.get()) { "O mecanismo de IA local está sendo encerrado." }

            val modelPath = modelRepository.installedModelPath()
                ?: throw IllegalStateException("Nenhum modelo local válido está instalado.")
            val modelFile = File(modelPath)
            val modelKey = listOf(
                modelPath,
                modelFile.length().toString(),
                modelFile.lastModified().toString(),
            ).joinToString(":")

            if (engine == null || loadedModelKey != modelKey) {
                releaseEngine()
                val newEngine = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = cacheDirectory,
                    ),
                )
                // Do not retain or close an engine when initialization fails.
                newEngine.initialize()
                engine = newEngine
                loadedModelKey = modelKey
            }

            val initialMessages = history
                .takeLast(LuoPromptBuilder.MAX_HISTORY_MESSAGES)
                .dropWhile { it.role == "assistant" }
                .mapNotNull { item ->
                    val bounded = LuoPromptBuilder.boundHistoryMessage(item.content)
                    if (bounded.isBlank()) {
                        null
                    } else {
                        when (item.role) {
                            "user" -> Message.user(bounded)
                            "assistant" -> Message.model(bounded)
                            else -> null
                        }
                    }
                }

            val config = ConversationConfig(
                systemInstruction = Contents.of(LuoPromptBuilder.systemInstruction(savedMemoryContext)),
                initialMessages = initialMessages,
                maxOutputToken = MAX_OUTPUT_TOKENS,
                thinkingConfig = ThinkingConfig(enableThinking = false),
            )

            val conversation = checkNotNull(engine).createConversation(config)
            activeConversation = conversation
            try {
                conversation.use { currentConversation ->
                    val answer = StringBuilder()
                    currentConversation.sendMessageAsync(
                        LuoPromptBuilder.boundUserPrompt(prompt),
                        maxOutputToken = MAX_OUTPUT_TOKENS,
                        thinkingConfig = ThinkingConfig(enableThinking = false),
                    ).collect { chunk ->
                        // LiteRT-LM streams Message objects; toString() returns their text contents.
                        answer.append(chunk.toString())
                    }
                    answer.toString().trim().also {
                        check(it.isNotBlank()) { "O modelo local não gerou uma resposta textual." }
                    }
                }
            } finally {
                if (activeConversation === conversation) activeConversation = null
            }
        }
    }

    /**
     * Stop active generation if necessary, wait for native resources to become idle, then unload.
     */
    suspend fun unload() {
        withContext(dispatcher) {
            activeConversation?.let { conversation ->
                runCatching { conversation.cancelProcess() }
            }
        }
        withContext(dispatcher) {
            mutex.withLock { releaseEngine() }
        }
    }

    private fun releaseEngine() {
        activeConversation?.let { conversation ->
            runCatching { conversation.cancelProcess() }
            runCatching { conversation.close() }
            activeConversation = null
        }
        engine?.let { loaded ->
            loaded.close()
        }
        engine = null
        loadedModelKey = null
    }

    /**
     * ViewModel cleanup can be called on the main thread. Request cancellation on the owner
     * executor and complete native cleanup asynchronously after the active turn leaves the mutex.
     */
    override fun close() {
        if (!closeRequested.compareAndSet(false, true)) return

        try {
            executor.execute {
                activeConversation?.let { conversation ->
                    runCatching { conversation.cancelProcess() }
                }
            }
            cleanupScope.launch {
                try {
                    mutex.withLock { releaseEngine() }
                } finally {
                    cleanupScope.cancel()
                    dispatcher.close()
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            dispatcher.close()
        }
    }

    private companion object {
        const val MAX_OUTPUT_TOKENS = 384
    }
}
