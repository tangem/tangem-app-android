package com.tangem.tap.features.sprinklr.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeActivity
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.tap.common.analytics.events.Chat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class SprinklrActivity : ComposeActivity() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    private val viewModel by viewModels<SprinklrViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.send(Chat.ScreenOpened())
        viewModel.setNavigateBackCallback {
            this.finish()
        }
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val state by viewModel.state.collectAsStateWithLifecycle()

        val systemBarsColor = TangemTheme.colors.background.primary
        SystemBarsEffect {
            setSystemBarsColor(systemBarsColor)
        }

        BackHandler(onBack = state.onNavigateBack)
        SprinklrScreenContent(
            modifier = modifier
                .systemBarsPadding()
                .imePadding(),
            state = state,
        )
    }
}