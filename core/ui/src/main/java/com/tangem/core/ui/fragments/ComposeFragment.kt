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
    protected abstract fun provideState(): State<ScreenState>

    @Composable
    protected abstract fun ScreenContent(
        modifier: Modifier,
        state: ScreenState,
    )
}
