package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class VibeAiRepository {
    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are VibeAI, a witty, culturally fluent social & conversation assistant inside the VIBE social app. Keep responses concise, expressive, natural, and ready to send."
                )
            }
        )
    }

    private val replySchema = Schema.obj(
        properties = mapOf(
            "replies" to Schema.array(
                items = Schema.string(),
                description = "Three short, natural reply suggestions under 12 words each"
            )
        )
    )

    private val smartReplyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = replySchema
            }
        )
    }

    suspend fun suggestReplies(recentMessages: List<String>, activeMood: String): Result<List<String>> {
        return try {
            val contextText = recentMessages.takeLast(6).joinToString("\n")
            val prompt = """
                Based on this recent chat snippet and the user's current mood ($activeMood),
                suggest 3 crisp, engaging reply options they can tap to send immediately.
                Chat:
                $contextText
            """.trimIndent()

            val response = withTimeout(45_000L) {
                smartReplyModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty AI response")
            val json = JSONObject(raw)
            val arr = json.getJSONArray("replies")
            val list = (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
            if (list.isEmpty()) throw IllegalStateException("No suggestions generated")
            Result.success(list)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Smart reply timed out", e)
            Result.failure(Exception("VibeAI timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Smart reply failed", e)
            Result.failure(e)
        }
    }

    fun streamAssistant(prompt: String): Flow<String> = flow {
        val acc = StringBuilder()
        try {
            withTimeout(60_000L) {
                chatModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        acc.append(piece)
                        emit(acc.toString())
                    }
                }
            }
            if (acc.isBlank()) {
                throw IllegalStateException("VibeAI returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI stream timed out", e)
            throw Exception("VibeAI timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI stream failed", e)
            throw e
        }
    }

    suspend fun generateCaptionAndHashtags(topic: String, mood: String): Result<String> {
        return try {
            val prompt = "Write 1 punchy social media caption for VIBE about '$topic' in a '$mood' mood, followed by 3 relevant #hashtags. Return only the caption and hashtags."
            val response = withTimeout(45_000L) {
                chatModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) Result.failure(Exception("Empty caption response"))
            else Result.success(text)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Caption generation timed out", e)
            Result.failure(Exception("VibeAI timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Caption generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun rewriteMessageTone(draft: String, tone: String): Result<String> {
        return try {
            val prompt = "Rewrite this chat message in a '$tone' vibe, keeping the same meaning and keeping it brief: \"$draft\". Return only the rewritten message."
            val response = withTimeout(45_000L) {
                chatModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) Result.failure(Exception("Empty rewrite response"))
            else Result.success(text)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Tone rewrite timed out", e)
            Result.failure(Exception("VibeAI timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tone rewrite failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "VibeAiRepository"
    }
}
