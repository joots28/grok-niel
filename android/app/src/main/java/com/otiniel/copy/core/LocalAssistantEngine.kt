package com.otiniel.copy.core

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.ThinkingConfig
import com.otiniel.copy.data.ConversationMessage
import com.otiniel.copy.data.LocalModelRepository
import java.io.File
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Free-first text inference that runs a user-imported LiteRT-LM model on the device.
 * No remote AI provider or network request is made by this class.
 */
class LocalAssistantEngine(
    context: Context,
    private val modelRepository: LocalModelRepository,
) : AutoCloseable {
    private val cacheDirectory = context.applicationContext.cacheDir.absolutePath
    private val mutex = Mutex()

    private var engine: Engine? = null
    private var loadedModelKey: String? = null

    suspend fun generate(
        prompt: String,
        savedMemoryContext: String,
        history: List<ConversationMessage>,
    ): String = mutex.withLock {
        val modelPath = modelRepository.installedModelPath()
            ?: throw IllegalStateException("Nenhum modelo local válido está instalado.")
        val modelFile = File(modelPath)
        val modelKey = listOf(
            modelPath,
            modelFile.length().toString(),
            modelFile.lastModified().toString(),
        ).joinToString(":")

        if (engine == null || loadedModelKey != modelKey) {
            engine?.close()
            engine = null
            val newEngine = Engine(
                EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    cacheDir = cacheDirectory,
                ),
            )
            newEngine.initialize()
            engine = newEngine
            loadedModelKey = modelKey
        }

        val initialMessages = history
            .takeLast(LuoPromptBuilder.MAX_HISTORY_MESSAGES)
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
            .dropWhile { it.role == "model" }

        val config = ConversationConfig(
            systemInstruction = Contents.of(LuoPromptBuilder.systemInstruction(savedMemoryContext)),
            initialMessages = initialMessages,
            maxOutputToken = MAX_OUTPUT_TOKENS,
            thinkingConfig = ThinkingConfig(enableThinking = false),
        )

        engine!!.createConversation(config).use { conversation ->
            val answer = StringBuilder()
            conversation.sendMessageAsync(
                LuoPromptBuilder.boundUserPrompt(prompt),
            ).collect { chunk ->
                answer.append(chunk.text)
            }
            answer.toString().trim().also {
                check(it.isNotBlank()) { "O modelo local não gerou uma resposta textual." }
            }
        }
    }

    override fun close() {
        engine?.close()
        engine = null
        loadedModelKey = null
    }

    private companion object {
        const val MAX_OUTPUT_TOKENS = 384
    }
}
