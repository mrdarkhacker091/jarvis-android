package com.jarvis.assistant.notifications

import android.content.Context

data class CapturedNotification(val id: Int, val pkg: String, val app: String, val sender: String?, val text: String, val at: Long, val canReply: Boolean)

object NotificationStore {
    private const val SP = "jarvis_notifications"; private const val KEY = "items"; private const val MAX = 60
    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(SP, Context.MODE_PRIVATE)
    fun add(ctx: Context, n: CapturedNotification) {
        val all = all(ctx).toMutableList(); all.add(n); while (all.size > MAX) all.removeAt(0); persist(ctx, all)
    }
    fun all(ctx: Context): List<CapturedNotification> = try {
        val arr = org.json.JSONArray(sp(ctx).getString(KEY, "[]") ?: "[]"); (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i); CapturedNotification(o.optInt("id"), o.optString("pkg"), o.optString("app"), o.optString("sender").ifBlank { null }, o.optString("text"), o.optLong("at"), o.optBoolean("canReply"))
        }
    } catch (e: Exception) { emptyList() }
    fun recent(ctx: Context, limit: Int) = all(ctx).takeLast(limit.coerceAtLeast(1)).reversed()
    fun clear(ctx: Context) { sp(ctx).edit().remove(KEY).apply() }
    private fun persist(ctx: Context, items: List<CapturedNotification>) {
        val arr = org.json.JSONArray(); items.forEach { arr.put(org.json.JSONObject().put("id", it.id).put("pkg", it.pkg).put("app", it.app).put("sender", it.sender ?: "").put("text", it.text).put("at", it.at).put("canReply", it.canReply)) }; sp(ctx).edit().putString(KEY, arr.toString()).apply()
    }
}
