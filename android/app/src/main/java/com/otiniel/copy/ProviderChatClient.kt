package com.otiniel.copy

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets

class ProviderRequestException(message: String) : IOException(message)

/**
 * Small, provider-neutral client for OpenAI-compatible chat-completions APIs.
 * It sends only chat text and never exposes provider credentials in error messages.
 */
object ProviderChatClient {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 90_000
    private const val MAX_RESPONSE_CHARS = 1_000_000
    private const val MAX_TURN_CHARS = 12_000

    fun complete(config: ModelProviderConfig, history: List<ChatMessage>): String {
        val endpoint = try {
            URL(ProviderConfigValidator.completionUrl(config.baseUrl).value)
        } catch (_: Exception) {
            throw ProviderRequestException("A URL do provedor não é válida.")
        }

        val turns = history.takeLast(16)
        if (turns.isEmpty() || turns.last().role != "user") {
            throw ProviderRequestException("Não há uma mensagem de usuário para enviar.")
        }
        if (turns.any { it.content.length > MAX_TURN_CHARS }) {
            throw ProviderRequestException("Uma mensagem é longa demais para o envio remoto. Reduza o texto e tente novamente.")
        }

        val requestJson = JSONObject()
            .put("model", config.model)
            .put("stream", false)
            .put("temperature", 0.7)
            .put("max_tokens", 1200)
            .put("messages", JSONArray().apply {
                put(
                    JSONObject()
                        .put("role", "system")
                        .put(
                            "content",
                            "Você é COPY, um assistente pessoal útil, honesto e cuidadoso. " +
                                "Responda em português brasileiro por padrão. " +
                                "Não afirme ter executado ações no dispositivo, acessado arquivos ou conectado serviços " +
                                "quando nenhuma ferramenta correspondente estiver disponível."
                        )
                )
                turns.forEach { turn ->
                    if (turn.role == "user" || turn.role == "assistant") {
                        put(JSONObject().put("role", turn.role).put("content", turn.content))
                    }
                }
            })

        val connection = try {
            endpoint.openConnection() as HttpURLConnection
        } catch (_: Exception) {
            throw ProviderRequestException("Não foi possível abrir uma conexão com o provedor.")
        }

        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer " + config.apiKey)
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Connection", "close")
            connection.outputStream.use { stream ->
                stream.write(requestJson.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                throw ProviderRequestException(
                    when (status) {
                        401, 403 -> "O provedor recusou a autenticação. Confira a chave e as permissões."
                        404 -> "O endpoint ou o modelo não foi encontrado. Confira a URL e o modelo configurados."
                        429 -> "O provedor aplicou um limite de uso. Confira a cota e tente novamente mais tarde."
                        in 500..599 -> "O serviço do provedor está indisponível no momento (HTTP $status)."
                        else -> "O provedor recusou a solicitação (HTTP $status)."
                    }
                )
            }

            val body = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                val output = StringBuilder()
                val buffer = CharArray(8192)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    if (output.length + count > MAX_RESPONSE_CHARS) {
                        throw ProviderRequestException("A resposta do provedor excedeu o limite permitido.")
                    }
                    output.append(buffer, 0, count)
                }
                output.toString()
            }

            val json = try {
                JSONObject(body)
            } catch (_: Exception) {
                throw ProviderRequestException("O provedor retornou uma resposta que não está em JSON válido.")
            }
            val choices = json.optJSONArray("choices")
                ?: throw ProviderRequestException("O provedor retornou um formato de resposta não reconhecido.")
            val message = choices.optJSONObject(0)?.optJSONObject("message")
                ?: throw ProviderRequestException("O provedor não retornou uma mensagem.")
            val content = extractText(message.opt("content")).trim()
            if (content.isBlank()) {
                throw ProviderRequestException("O provedor retornou uma resposta vazia.")
            }
            return content
        } catch (error: ProviderRequestException) {
            throw error
        } catch (_: SocketTimeoutException) {
            throw ProviderRequestException("O provedor demorou demais para responder. Tente novamente.")
        } catch (_: Exception) {
            throw ProviderRequestException(
                "Não foi possível concluir a solicitação. Confira a conexão e a configuração do provedor."
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun extractText(content: Any?): String {
        return when (content) {
            is String -> content
            is JSONArray -> buildString {
                for (index in 0 until content.length()) {
                    val part = content.optJSONObject(index) ?: continue
                    if (part.optString("type") == "text") {
                        append(part.optString("text"))
                    }
                }
            }
            else -> ""
        }
    }
}
