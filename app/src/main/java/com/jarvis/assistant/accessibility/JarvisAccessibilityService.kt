package com.jarvis.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun performNamedAction(action: String): String {
        val mapped = when (action.lowercase().trim()) {
            "back" -> GLOBAL_ACTION_BACK; "home" -> GLOBAL_ACTION_HOME; "recents", "recent apps" -> GLOBAL_ACTION_RECENTS
            "notifications", "shade" -> GLOBAL_ACTION_NOTIFICATIONS; "quick_settings", "quick settings", "settings panel" -> GLOBAL_ACTION_QUICK_SETTINGS
            else -> return "I do not know the action $action."
        }; val ok = performGlobalAction(mapped); return if (ok) "Done: $action." else "The system refused $action."
    }

    fun toggleQuickSetting(label: String): String {
        val wanted = label.trim().lowercase(); if (wanted.isEmpty()) return "Which setting?"
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS); Thread.sleep(900)
        var root = rootInActiveWindow; var tries = 0
        while (root == null && tries < 4) { Thread.sleep(300); root = rootInActiveWindow; tries++ }
        if (root == null) { performGlobalAction(GLOBAL_ACTION_BACK); return "Could not read quick settings." }
        val node = findNode(root, wanted, 0); if (node == null) { performGlobalAction(GLOBAL_ACTION_BACK); return "Could not find $label." }
        val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK); performGlobalAction(GLOBAL_ACTION_BACK)
        return if (clicked) "Toggled $label." else "Found $label but tap failed."
    }

    private fun findNode(node: AccessibilityNodeInfo?, wanted: String, depth: Int): AccessibilityNodeInfo? {
        if (node == null || depth > 24) return null
        val text = node.text?.toString()?.lowercase().orEmpty(); val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        if (text == wanted || desc == wanted || text.contains(wanted) || desc.contains(wanted)) return node
        for (i in 0 until node.childCount) { val f = findNode(node.getChild(i), wanted, depth + 1); if (f != null) return f }
        return null
    }

    companion object { @Volatile var instance: JarvisAccessibilityService? = null }
}
