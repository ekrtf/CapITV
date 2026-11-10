package com.ekrtf.capitv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private lateinit var config: AppConfig
    private lateinit var resolver: LaunchResolver

    /** Bumped on every resume so tiles re-check which apps are installed. */
    private val resumeTick = mutableIntStateOf(0)
    private val messages = MessageBus()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = AppConfig.parse(assets.open(CONFIG_ASSET).bufferedReader().use { it.readText() })
        resolver = LaunchResolver(config, ::isInstalled)

        // This is the home screen: Back has nowhere to go.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        setContent {
            HomeScreen(
                config = config,
                resolver = resolver,
                resumeTick = resumeTick.intValue,
                messages = messages,
                onLaunch = ::launchTile,
                onSettings = ::openSettings,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }

    // Keys the focused tile does not use (digits, colour keys, app buttons) end up here.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount == 0) {
            when (val action = resolver.actionForKey(keyCode)) {
                is KeyAction.LaunchTile -> resolver.tile(action.tileId)?.let { launchTile(it); return true }
                KeyAction.OpenSettings -> { openSettings(); return true }
                null -> Unit
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isInstalled(pkg: String): Boolean = try {
        packageManager.getApplicationInfo(pkg, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun launchTile(tile: Tile) {
        val pkg = resolver.resolve(tile)
        if (pkg == null) {
            messages.show(getString(R.string.not_installed, tile.label))
            openStore(resolver.storePackageFor(tile))
            return
        }
        val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            messages.show(getString(R.string.cannot_open, tile.label))
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            messages.show(getString(R.string.cannot_open, tile.label))
        } catch (_: SecurityException) {
            messages.show(getString(R.string.cannot_open, tile.label))
        }
    }

    private fun openStore(pkg: String) {
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(market)
        } catch (_: ActivityNotFoundException) {
            // No Play Store: the on-screen message is all we can do.
        }
    }

    private fun openSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            messages.show(getString(R.string.cannot_open, getString(R.string.settings)))
        }
    }

    companion object {
        const val CONFIG_ASSET = "capitv.json"
    }
}
