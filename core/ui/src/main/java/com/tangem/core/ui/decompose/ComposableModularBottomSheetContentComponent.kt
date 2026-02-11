package com.tangem.core.ui.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState

/**
 * An interface describing the UI part of a component for a modular BottomSheet.
 *
 * Designed for use in Decompose components. It separates the UI into a title and content,
 * providing access to the [BottomSheetState] to react to changes in the sheet's state (collapsed/expanded).
 */
@Stable
interface ComposableModularBottomSheetContentComponent {

    /**
     * Renders the title of the bottom sheet.
     * @param bottomSheetState The current state of the bottom sheet. This can be used, for example,
     * to change navigation buttons (e.g., hiding the "Back" button when collapsed).
     */
    @Composable
    fun Title(bottomSheetState: State<BottomSheetState>)

    /**
     * Renders the main content of the bottom sheet.
     * @param bottomSheetState The current state of the bottom sheet. Useful for tracking visibility
     * (e.g., for analytics or lifecycle effects when the sheet is [BottomSheetState.EXPANDED]).
     */
    @Composable
    fun Content(bottomSheetState: State<BottomSheetState>, modifier: Modifier)
}