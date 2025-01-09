package com.tangem.core.ui.components.bottomsheets

/**
 * Tangem bottom sheet config
 *
 * @property isShown           flag that determine if bottom sheet is shown
 * @property onDismissRequest lambda be invoked when bottom sheet is dismissed
 * @property content          content config
 */
data class TangemBottomSheetConfig(
    val isShown: Boolean,
    val onDismissRequest: () -> Unit,
    val content: TangemBottomSheetConfigContent,
) {

    companion object {
        val Empty = TangemBottomSheetConfig(false, {}, TangemBottomSheetConfigContent.Empty)
    }
}