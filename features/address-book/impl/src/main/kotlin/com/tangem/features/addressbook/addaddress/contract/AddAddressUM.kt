package com.tangem.features.addressbook.addaddress.contract

import androidx.annotation.DrawableRes
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.domain.models.network.Network
import kotlinx.collections.immutable.ImmutableList

internal data class AddAddressUM(
    val addressField: AddressFieldUM,
    val availableNetworks: ImmutableList<Network>,
    val buttonUM: TangemButtonUM,
    val chosenNetworkState: ChosenNetworkState,
    val onAddressChange: (String) -> Unit,
    val onAddressClear: () -> Unit,
    val onPasteClick: () -> Unit,
    val onQrClick: () -> Unit,
    val onBackClick: () -> Unit,
) {
    sealed class ChosenNetworkState {
        data object Loading : ChosenNetworkState()
        data object Empty : ChosenNetworkState()
        data class Result(
            val networkUMList: ImmutableList<NetworkUM>,
        ) : ChosenNetworkState() {

            data class NetworkUM(
                val networkName: String,
                @DrawableRes val iconResId: Int,
            )
        }
    }
}