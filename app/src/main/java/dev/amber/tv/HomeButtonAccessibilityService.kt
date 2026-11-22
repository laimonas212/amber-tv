package dev.amber.tv

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Philips / Google TV firmware can route HOME and wake events to its stock
 * launcher even when Amber TV owns the Android HOME role. This optional,
 * user-enabled accessibility service redirects those events to Amber TV.
 */
class HomeButtonAccessibilityService : AccessibilityService() {
    private var lastRedirectAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName != "com.google.android.apps.tv.launcherx" &&
            packageName != "org.droidtv.homeintentresolver"
        ) return

        openAmberHome()
    }

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME || event.action != KeyEvent.ACTION_UP) {
            return false
        }
        openAmberHome()
        return true
    }

    private fun openAmberHome() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRedirectAt < 1200L) return
        lastRedirectAt = now

        val launch = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(launch)
    }
}
