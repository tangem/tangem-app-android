package com.tangem.core.ui.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

abstract class ComposeBottomSheetFragment<ScreenState> : BottomSheetDialogFragment() {
    open val initialBottomSheetState = BottomSheetBehavior.STATE_EXPANDED

    @FloatRange(from = 0.0, to = 1.0)
    open val expandedHeightFraction: Float? = null

    override fun getTheme(): Int = R.style.AppTheme_TransparentBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return createComposeView(inflater.context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        (dialog as BottomSheetDialog).behavior.apply {
            state = initialBottomSheetState
            skipCollapsed = true
        }

        return dialog
    }

    private fun createComposeView(context: Context): View {
        return ComposeView(context).apply {
            setContent {
                TangemTheme {
                    ScreenContent(
                        state = provideState().value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .let {
                                if (expandedHeightFraction != null) it.fillMaxHeight(expandedHeightFraction!!) else it
                            }
                            .background(
                                color = TangemTheme.colors.background.plain,
                                shape = TangemTheme.shapes.bottomSheet,
                            ),
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
    protected abstract fun ScreenContent(
        state: ScreenState,
        modifier: Modifier,
    )
}
