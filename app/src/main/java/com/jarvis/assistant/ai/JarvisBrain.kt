package com.jarvis.assistant.ai

import android.content.Context
import com.jarvis.assistant.JarvisBus
import com.jarvis.assistant.data.Memory
import com.jarvis.assistant.data.Prefs
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JarvisBrain {

    private const val MAX_ROUNDS = 4

    fun respond(ctx: Context, userText: String): String {
        val prefs = Prefs.get(ctx)
        if (prefs.apiKey.isBlank() || prefs.baseUrl.isBlank()) {
            return "I do not have a model key yet. Open settings and paste one in."
        }
        val client = LlmClient(prefs.baseUrl, prefs.apiKey, prefs.model)

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt(ctx)))

        JarvisBus.turns.value.takeLast(12).forEach { t ->
            val role = if (t.from == "user") "user" else "assistant"
            messages.put(JSONObject().put("role", role).put("content", t.text))
        }
        messages.put(JSONObject().put("role", "user").put("content", userText))

        val tools = ToolRegistry.schema()

        repeat(MAX_ROUNDS) {
            val reply = try {
                client.complete(messages, tools)
            } catch (e: Exception) {
                return "I could not reach my brain just now. " +
                    (e.message?.take(120) ?: "Connection problem.")
            }

            if (reply.toolCalls.isEmpty()) {
                return reply.text?.trim().orEmpty().ifBlank { "I did not get a usable answer." }
            }

            val assistant = JSONObject()
                .put("role", "assistant")
                .put("content", reply.text ?: "")

            val calls = JSONArray()
            reply.toolCalls.forEach { call ->
                calls.put(
                    JSONObject()
                        .put("id", call.id)
                        .put("type", "function")
                        .put("function", JSONObject()
                            .put("name", call.name)
                            .put("arguments", call.args.toString()))
                )
            }
            assistant.put("tool_calls", calls)
            messages.put(assistant)

            reply.toolCalls.forEach { call ->
                val result = ToolRegistry.execute(ctx, call.name, call.args)
                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.id)
                        .put("content", result)
                )
            }
        }

        return "I went a few rounds without settling on an answer. Try asking more directly."
    }

    private fun systemPrompt(ctx: Context): String {
        val prefs = Prefs.get(ctx)
        val now = SimpleDateFormat("EEEE d MMMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        return """
You are JARVIS, a voice assistant running on the user's Android phone.

Current time: $now
Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}
Notification announcements: ${if (prefs.announceNotifications) "on" else "off"}
Quiet hours: ${prefs.quietStart}:00 to ${prefs.quietEnd}:00

Facts the user has approved you remembering:
${Memory.toPromptString(ctx)}

How to behave:
- Speak, do not write. Answers are read aloud, so keep them to one or two sentences unless asked to elaborate.
- No markdown, no bullet lists, no asterisks, no emoji. Plain spoken English.
- If the user asks you to do something and a tool can do it, call the tool. Do not claim an action you did not call a tool for.
- If a tool fails or a permission is missing, say exactly that in one sentence and tell the user what to grant.
- You can only read notification previews, not full chat history. Never pretend otherwise.
- If a request is ambiguous and acting would be destructive, ask one short clarifying question instead of guessing.
- Never invent device state. Read it with a tool or say you do not know.
""".trimIndent()
    }
}
