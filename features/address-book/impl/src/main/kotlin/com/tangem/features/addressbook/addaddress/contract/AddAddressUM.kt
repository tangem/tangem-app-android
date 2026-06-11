package com.tangem.features.addressbook.addaddress.contract

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.domain.models.network.Network
import kotlinx.collections.immutable.ImmutableList

internal data class AddAddressUM(
    val addressField: AddressFieldUM,
    val availableNetworks: ImmutableList<Network>,
    val buttonUM: TangemButtonUM,
    val chosenNetworkStateUM: ChosenNetworkStateUM,
    val onAddressChange: (String) -> Unit,
    val onAddressClear: () -> Unit,
    val onPasteClick: () -> Unit,
    val onQrClick: () -> Unit,
    val onBackClick: () -> Unit,
) {
    @Immutable
    sealed class ChosenNetworkStateUM {
        data object Loading : ChosenNetworkStateUM()
        data object Empty : ChosenNetworkStateUM()
        data class Result(
            val networkUMList: ImmutableList<NetworkUM>,
        ) : ChosenNetworkStateUM() {

            data class NetworkUM(
                val networkName: String,
                @DrawableRes val iconResId: Int,
            )
        }
    }
}