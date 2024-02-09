package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

/**
 * Config for the token actions bottom sheet
 *
 * @property actions actions
 */
internal data class ActionsBottomSheetConfig(
    val actions: ImmutableList<TokenActionButtonConfig>,
) : TangemBottomSheetConfigContent