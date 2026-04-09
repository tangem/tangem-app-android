package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.R as CoreR

@Composable
internal fun DynamicAddressesBottomSheet(config: TangemBottomSheetConfig) {
    TangemModalBottomSheet<DynamicAddressesBottomSheetConfig>(
        config = config,
        title = {
            TangemModalBottomSheetTitle(
                endIconRes = CoreR.drawable.ic_close_24,
                onEndClick = config.onDismissRequest,
            )
        },
    ) { content ->
        when (content) {
            is DynamicAddressesBottomSheetConfig.Enable -> DynamicAddressesEnableContent(content = content)
            is DynamicAddressesBottomSheetConfig.Unavailable -> DynamicAddressesUnavailableContent(content = content)
            is DynamicAddressesBottomSheetConfig.ServiceUnavailable -> DynamicAddressesServiceUnavailableContent(
                content = content,
            )
        }
    }
}