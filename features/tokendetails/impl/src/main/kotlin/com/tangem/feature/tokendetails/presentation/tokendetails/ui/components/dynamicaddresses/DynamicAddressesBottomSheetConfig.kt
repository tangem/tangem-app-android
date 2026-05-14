package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

@Immutable
internal sealed class DynamicAddressesBottomSheetConfig : TangemBottomSheetConfigContent {

    data class Enable(
        @DrawableRes val iconRes: Int? = null,
        val isLoading: Boolean = false,
        val onEnableClick: () -> Unit,
    ) : DynamicAddressesBottomSheetConfig()

    data class DisableWithoutConsolidation(
        val onDisableClick: () -> Unit,
        val onReadMoreClick: () -> Unit,
    ) : DynamicAddressesBottomSheetConfig()

    data class DisableWithConsolidation(
        @DrawableRes val iconRes: Int? = null,
        val isHoldToConfirm: Boolean = false,
        val feeState: DisableFeeState = DisableFeeState.Loading,
        val isSending: Boolean = false,
        val onDisableClick: () -> Unit,
        val onRefreshFee: () -> Unit,
        val onReadMoreClick: () -> Unit,
    ) : DynamicAddressesBottomSheetConfig()

    sealed interface DisableFeeState {
        data object Loading : DisableFeeState
        data class Content(
            val feeSymbol: String,
            val fiatFormatted: String,
        ) : DisableFeeState
        data object Error : DisableFeeState
    }

    data class ConflictingCustomTokens(
        val onDismissClick: () -> Unit,
    ) : DynamicAddressesBottomSheetConfig()

    data class ServiceUnavailable(
        val onDismissClick: () -> Unit,
    ) : DynamicAddressesBottomSheetConfig()
}