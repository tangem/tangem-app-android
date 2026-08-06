package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun DynamicAddressesBottomSheet(config: TangemBottomSheetConfig) {
    TangemModalBottomSheet<DynamicAddressesBottomSheetConfig>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            TangemTopNavigation(
                windowInsets = WindowInsets(0),
                blurBackground = false,
                endButton = { TangemButton.Close(onClick = config.onDismissRequest) },
            )
        },
    ) { content ->
        when (content) {
            is DynamicAddressesBottomSheetConfig.Enable -> DynamicAddressesEnableContent(content = content)
            is DynamicAddressesBottomSheetConfig.DisableWithoutConsolidation -> {
                DynamicAddressesDisableWithoutConsolidationContent(content = content)
            }
            is DynamicAddressesBottomSheetConfig.DisableWithConsolidation -> {
                DynamicAddressesDisableWithConsolidationContent(content = content)
            }
            is DynamicAddressesBottomSheetConfig.ConflictingCustomTokens -> {
                DynamicAddressesConflictingCustomTokensContent(content = content)
            }
            is DynamicAddressesBottomSheetConfig.ServiceUnavailable -> {
                DynamicAddressesServiceUnavailableContent(content = content)
            }
        }
    }
}