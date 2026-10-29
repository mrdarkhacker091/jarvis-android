package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.jarvis.assistant.data.Memory
import com.jarvis.assistant.data.Prefs
import com.jarvis.assistant.services.JarvisForegroundService
import com.jarvis.assistant.ui.HomeScreen
import com.jarvis.assistant.ui.JarvisTheme
import com.jarvis.assistant.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val runtimePerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        askForRuntimePermissions()
        setContent {
            JarvisTheme {
                val turns by JarvisBus.turns.collectAsState()
                val state by JarvisBus.state.collectAsState()
                var showSettings by remember { mutableStateOf(false) }

                val lifecycleOwner = LocalLifecycleOwner.current
                var resumeTick by remember { mutableStateOf(0) }
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) resumeTick += 1
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val warning = buildString {
                    val missing = mutableListOf<String>()
                    if (!hasMic()) missing.add("microphone")
                    if (!hasNotificationAccess()) missing.add("notification access")
                    if (!hasAccessibility()) missing.add("accessibility")
                    if (missing.isEmpty()) append("") else {
                        append("Missing: "); append(missing.joinToString(", ")); append(". Grant them in settings.")
                    }
                }.ifBlank { null }

                @Suppress("UNUSED_EXPRESSION")
                resumeTick

                if (showSettings) {
                    SettingsScreen(
                        prefs = Prefs.get(this@MainActivity),
                        onBack = { showSettings = false },
                        onGrantNotificationAccess = { openNotificationAccess() },
                        onGrantAccessibility = { openAccessibility() },
                        onGrantWriteSettings = { openWriteSettings() },
                        onGrantOverlay = { openOverlay() },
                        onBatteryExemption = { openBattery() },
                        onClearMemory = { Memory.clear(this@MainActivity); JarvisBus.clear() },
                        onRestartService = { restartService() }
                    )
                } else {
                    HomeScreen(
                        turns = turns, state = state,
                        onTalk = { JarvisForegroundService.listenOnce(this@MainActivity) },
                        onSend = { text -> JarvisForegroundService.process(this@MainActivity, text) },
                        onSettings = { showSettings = true },
                        onClear = { JarvisBus.clear() },
                        setupWarning = warning
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try { JarvisForegroundService.start(this) } catch (e: Exception) { }
    }

    private fun askForRuntimePermissions() {
        val wanted = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) wanted.add(Manifest.permission.POST_NOTIFICATIONS)
        wanted.add(Manifest.permission.CAMERA)
        runtimePerms.launch(wanted.toTypedArray())
    }

    private fun hasMic(): Boolean =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun hasNotificationAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    private fun hasAccessibility(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.contains(packageName) }
    }

    private fun openNotificationAccess() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun openAccessibility() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openWriteSettings() {
        startActivity(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
        else Intent(Settings.ACTION_SETTINGS))
    }

    private fun openOverlay() {
        startActivity(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        else Intent(Settings.ACTION_SETTINGS))
    }

    private fun openBattery() {
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:$packageName")))
    }

    private fun restartService() {
        try { JarvisForegroundService.stop(this); JarvisForegroundService.start(this) } catch (e: Exception) { }
    }
}
