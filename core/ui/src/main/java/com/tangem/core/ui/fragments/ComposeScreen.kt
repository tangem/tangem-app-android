package com.tangem.core.ui.fragments

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme

internal interface ComposeScreen<ScreenState> {
    fun createComposeView(context: Context): View {
        return ComposeView(context).apply {
            setContent {
                TangemTheme {
                    val backgroundColor = TangemTheme.colors.background.primary
                    SystemBarsEffect {
                        setSystemBarsColor(
                            color = backgroundColor,
                        )
                    }

                    ScreenContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = backgroundColor),
                        state = provideState().value,
                    )
                }
            }
        }
    }

    @Composable
    fun provideState(): State<ScreenState>

    @Composable
    fun ScreenContent(
        modifier: Modifier,
        state: ScreenState,
    )
}