package com.tangem.features.send.impl.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.screen.ComposeBottomSheetFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.features.send.impl.presentation.send.state.SendUiState
import com.tangem.features.send.impl.presentation.send.ui.SendScreen
import com.tangem.features.send.impl.presentation.send.viewmodel.SendViewModel
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
        val viewModel = hiltViewModel<SendViewModel>()
        LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)

        SystemBarsEffect { setSystemBarsColor(color = Color.Transparent) }
        BackHandler { dismiss() }

        when (val state = viewModel.uiState) {
            is SendUiState.Content -> SendScreen(state)
            SendUiState.Dismiss -> dismiss()
        }
    }

    companion object {

        /** Create send fragment instance */
        fun create(): SendFragment = SendFragment()
    }
}