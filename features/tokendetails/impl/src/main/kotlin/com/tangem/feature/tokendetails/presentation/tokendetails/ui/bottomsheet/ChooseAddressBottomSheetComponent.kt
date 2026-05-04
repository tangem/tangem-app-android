package com.tangem.feature.tokendetails.presentation.tokendetails.ui.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tangem.common.ui.bottomsheet.chooseaddress.ChooseAddressBottomSheet
import com.tangem.common.ui.bottomsheet.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.common.ui.bottomsheet.receive.AddressModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress

internal class ChooseAddressBottomSheetComponent(
    private val currency: CryptoCurrency,
    private val networkAddress: NetworkAddress,
    private val onAddressSelected: (AddressModel) -> Unit,
    private val onDismiss: () -> Unit,
) : ComposableBottomSheetComponent {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val config = remember(this) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = ChooseAddressBottomSheetConfig(
                    currency = currency,
                    networkAddress = networkAddress,
                    onClick = onAddressSelected,
                ),
            )
        }
        ChooseAddressBottomSheet(config = config)
    }
}