package com.tangem.features.addressbook.addaddress.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class AddAddressUM(
    val addressField: AddressFieldUM,
    val buttonUM: TangemButtonUM,
    val chosenNetworkStateUM: ChosenNetworkStateUM,
    val onAddressChange: (String) -> Unit,
    val onAddressClear: () -> Unit,
    val onPasteClick: () -> Unit,
    val onQrClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onNetworkClick: () -> Unit,
) {
    @Immutable
    sealed interface ChosenNetworkStateUM {
        data object Loading : ChosenNetworkStateUM
        data object Empty : ChosenNetworkStateUM

        data class Result(val networkUMList: ImmutableList<NetworkUM>) : ChosenNetworkStateUM {
            data class NetworkUM(
                val networkName: String,
                @DrawableRes val iconResId: Int,
            )
        }
    }
}