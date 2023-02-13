package com.tangem.core.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme

abstract class ComposeFragment<ScreenState> : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return createComposeView(inflater.context)
    }

    private fun createComposeView(context: Context): View {
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
                        state = provideState().value,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = backgroundColor),
                    )
                }
            }
        }
    }

    @Suppress("TopLevelComposableFunctions")
    @Composable
    protected abstract fun provideState(): State<ScreenState>

    @Suppress("TopLevelComposableFunctions")
    @Composable
    protected abstract fun ScreenContent(state: ScreenState, modifier: Modifier)
}
