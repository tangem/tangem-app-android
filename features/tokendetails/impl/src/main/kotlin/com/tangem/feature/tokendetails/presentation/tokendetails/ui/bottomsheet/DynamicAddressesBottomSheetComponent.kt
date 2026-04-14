package com.tangem.feature.tokendetails.presentation.tokendetails.ui.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.feature.tokendetails.presentation.tokendetails.model.DynamicAddressesDelegate
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses.DynamicAddressesBottomSheet

internal class DynamicAddressesBottomSheetComponent(
    private val dynamicAddressesDelegate: DynamicAddressesDelegate,
    private val onDismiss: () -> Unit,
) : ComposableBottomSheetComponent {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val content by dynamicAddressesDelegate.bottomSheetConfig.collectAsStateWithLifecycle()

        val config = remember(content) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = content,
            )
        }
        DynamicAddressesBottomSheet(config = config)
    }
}