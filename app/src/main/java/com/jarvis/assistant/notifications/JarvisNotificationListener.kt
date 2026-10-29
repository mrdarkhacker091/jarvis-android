package com.jarvis.assistant.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jarvis.assistant.R
import com.jarvis.assistant.data.Prefs
import com.jarvis.assistant.voice.TtsManager

class JarvisNotificationListener : NotificationListenerService() {
    private var tts: TtsManager? = null
    override fun onCreate() { super.onCreate(); Prefs.init(this); ensureChannel(); tts = TtsManager(this) }
    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
    override fun onListenerConnected() { instance = this }
    override fun onListenerDisconnected() { if (instance === this) instance = null }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return; if (n.packageName == packageName) return
        val prefs = Prefs.get(this); if (!prefs.isPackageAllowed(n.packageName)) return
        val extras = n.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty()
        if (title.isBlank() && body.isBlank()) return
        val app = appLabel(n.packageName); val canReply = hasReplyAction(n)
        NotificationStore.add(this, CapturedNotification(n.id, n.packageName, app, title.ifBlank { null }, body.ifBlank { title }, n.postTime, canReply))
        if (prefs.announceNotifications && !prefs.inQuietHours()) tts?.speak("$app${if (title.isNotEmpty()) ", from $title" else ""}. ${body.ifEmpty { title }}")
    }

    fun sendReply(senderHint: String, text: String): String {
        val active = try { activeNotifications ?: return "No notification access." } catch (e: Exception) { return "No notification access." }
        val hint = senderHint.trim().lowercase(); val candidates = active.filter { n ->
            val e = n.notification?.extras ?: return@filter false; val t = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(); val b = e.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(); hint.isEmpty() || t.lowercase().contains(hint) || b.lowercase().contains(hint) || appLabel(n.packageName).lowercase().contains(hint)
        }.sortedByDescending { it.postTime }
        if (candidates.isEmpty()) return "No current notification matches $senderHint."
        for (sbn in candidates) { for (action in sbn.notification?.actions ?: continue) {
            val inputs = action.remoteInputs ?: continue; if (inputs.isEmpty()) continue
            val bundle = android.os.Bundle(); inputs.forEach { ri -> bundle.putCharSequence(ri.resultKey, text) }
            val i = Intent(); RemoteInput.addResultsToIntent(inputs, i, bundle)
            return try { action.actionIntent.send(this, 0, i); "Reply sent." } catch (e: Exception) { "Could not send reply." }
        } }
        return "That notification has no reply action."
    }

    private fun hasReplyAction(n: StatusBarNotification) = n.notification?.actions?.any { it.remoteInputs != null } == true
    private fun appLabel(pkg: String) = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { pkg }
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL) != null) return
        mgr.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.notification_channel_messages), NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        private const val CHANNEL = "jarvis_messages"
        @Volatile var instance: JarvisNotificationListener? = null
        fun reply(ctx: Context, senderHint: String, text: String): String {
            val svc = instance ?: return "Notification access not connected."
            return svc.sendReply(senderHint, text)
        }
    }
}
