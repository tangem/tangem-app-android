package com.tangem.feature.wallet.presentation.wallet.state

import kotlinx.collections.immutable.ImmutableList

/**
 * Config for the token actions bottom sheet
 *
 * @property isShow           flag that determine if bottom sheet is shown
 * @property onDismissRequest lambda be invoked when bottom sheet is dismissed
 * @property actions          actions
 *
 */
internal data class ActionsBottomSheetConfig(
    val isShow: Boolean,
    val onDismissRequest: () -> Unit,
    val actions: ImmutableList<TokenActionButtonConfig>,
)