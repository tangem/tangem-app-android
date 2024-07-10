package com.tangem.core.ui.screen

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

/**
 * An abstract base class for bottom sheet dialogs that use Compose for UI rendering.
 * Extends [BottomSheetDialogFragment] and implements [ComposeScreen] interface.
 */
abstract class ComposeBottomSheetFragment : BottomSheetDialogFragment(), ComposeScreen {

    /**
     * The initial state of the bottom sheet. Default is [BottomSheetBehavior.STATE_EXPANDED].
     */
    open val initialBottomSheetState = BottomSheetBehavior.STATE_EXPANDED

    /**
     * The fraction of the screen height that the bottom sheet should take when expanded.
     * Default is `null`, indicating that the height will be determined by the content.
     */
    @FloatRange(from = 0.0, to = 1.0)
    open val expandedHeightFraction: Float? = null

    override val screenModifier: Modifier
        @Composable
        @ReadOnlyComposable
        get() = Modifier
            .fillMaxWidth()
            .let {
                if (expandedHeightFraction != null) it.fillMaxHeight(expandedHeightFraction!!) else it
            }
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.bottomSheet,
            )

    override fun getTheme(): Int = R.style.AppTheme_TransparentBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return createComposeView(inflater.context, requireActivity())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        (dialog as BottomSheetDialog).behavior.apply {
            state = initialBottomSheetState
            skipCollapsed = true
        }

        return dialog
    }
}
