package com.tangem.features.send.impl.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.screen.ComposeBottomSheetFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.features.send.impl.presentation.send.ui.SendScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Send fragment
 */
@AndroidEntryPoint
internal class SendFragment : ComposeBottomSheetFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    override val expandedHeightFraction: Float = 1f

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        SystemBarsEffect {
            setSystemBarsColor(color = Color.Transparent)
        }
        BackHandler {
            dismiss()
        }
        SendScreen()
    }

    companion object {

        /** Create send fragment instance */
        fun create(): SendFragment = SendFragment()
    }
}
