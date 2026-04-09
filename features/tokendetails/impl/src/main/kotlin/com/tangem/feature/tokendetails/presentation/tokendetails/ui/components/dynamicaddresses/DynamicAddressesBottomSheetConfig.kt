package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

@Immutable
internal sealed class DynamicAddressesBottomSheetConfig : TangemBottomSheetConfigContent {

    data class Enable(
        val isCardScanRequired: Boolean,
        val isLoading: Boolean = false,
        val onEnableClick: () -> Unit,
    ) : DynamicAddressesBottomSheetConfig()

    data class Unavailable(
        val onGotItClick: () -> Unit,
    ) : DynamicAddressesBottomSheetConfig()

    data class ServiceUnavailable(
        val onGotItClick: () -> Unit,
    ) : DynamicAddressesBottomSheetConfig()
}