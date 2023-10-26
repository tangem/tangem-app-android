package com.tangem.core.ui.components.bottomsheets

/**
 * Tangem bottom sheet config
 *
 * @property isShow           flag that determine if bottom sheet is shown
 * @property onDismissRequest lambda be invoked when bottom sheet is dismissed
 * @property content          content config
 */
data class TangemBottomSheetConfig(
    val isShow: Boolean,
    val onDismissRequest: () -> Unit,
    val content: TangemBottomSheetConfigContent,
)
