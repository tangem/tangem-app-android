package com.tangem.feature.tester.presentation.navigation

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.TesterActivity
import com.tangem.features.tester.api.TesterMenuLauncher

/**
 * Default implementation of [TesterMenuLauncher] that listens for double-press events
 * of the volume down button. When a double press is detected, it opens the tester menu.
 * Also publishes an app launcher shortcut (long tap on the app icon) that opens the tester menu.
 *
 * @param context the context used to launch the tester menu
 */
internal class DefaultTesterMenuLauncher(private val context: Context) : TesterMenuLauncher {

    override val launchOnKeyEventObserver = VolumeButtonDoublePressObserver(context)

    override fun registerTesterMenuShortcut() {
        // An action is mandatory for a shortcut intent (ShortcutInfoCompat requires it), unlike the
        // volume-button launch path which starts TesterActivity by explicit component only.
        val intent = Intent(context, TesterActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val label = context.getString(R.string.tester_menu)
        val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, com.tangem.core.ui.R.drawable.ic_gear_24))
            .setIntent(intent)
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    private companion object {
        const val SHORTCUT_ID = "tester_menu"
    }
}