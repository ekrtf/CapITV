package com.ekrtf.capitv

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Google TV has no default-launcher setting, so the stock launcher keeps the HOME role.
 * This service intercepts only KEYCODE_HOME and brings CapITV to the front instead.
 *
 * - It never reads window content (canRetrieveWindowContent=false) and subscribes to no
 *   accessibility events, so it does not even learn which app is in front.
 * - Every other key passes through untouched, so the remote's Netflix / YouTube /
 *   custom buttons keep working through the system.
 * - It opens CapITV with a private action rather than ACTION_MAIN/CATEGORY_HOME, so the
 *   stock launcher stays the system "home process" and keeps its background-start
 *   exemption (which is what makes the remote's app buttons work).
 * - Turning it off (Settings > Accessibility, or `capitv-provision.sh --restore`)
 *   gives the stock Home screen back immediately.
 */
class HomeKeyService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
            val intent = Intent(ACTION_SHOW_HOME)
                .setPackage(packageName)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
            runCatching { startActivity(intent) }
        }
        // Consume both DOWN and UP so the stock launcher does not also open.
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        const val ACTION_SHOW_HOME = "com.ekrtf.capitv.SHOW_HOME"
    }
}
