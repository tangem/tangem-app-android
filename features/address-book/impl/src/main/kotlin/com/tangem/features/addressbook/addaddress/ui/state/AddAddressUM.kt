package com.tangem.features.addressbook.addaddress.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class AddAddressUM(
    val addressField: AddressFieldUM,
    val memoField: MemoFieldUM,
    val buttonUM: TangemButtonUM,
    val chosenNetworkStateUM: ChosenNetworkStateUM,
    val onAddressChange: (String) -> Unit,
    val onAddressClear: () -> Unit,
    val onPasteClick: () -> Unit,
    val onQrClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onNetworkClick: () -> Unit,
) {

    /**
     * Optional memo / destination-tag input shown below the address only when a chosen network supports transaction
     * extras. [isVisible] toggles the whole field; [label] adapts to memo vs destination tag.
     */
    @Immutable
    data class MemoFieldUM(
        val isVisible: Boolean,
        val value: String,
        val label: TextReference,
        val isError: Boolean,
        val onValueChange: (String) -> Unit,
        val onPasteClick: () -> Unit,
    )

    @Immutable
    sealed interface ChosenNetworkStateUM {

        /** No address entered yet, or the address matched nothing — the network selector is not shown. */
        data object Hidden : ChosenNetworkStateUM

        /** A non-blank address is being validated against the supported networks. */
        data object Loading : ChosenNetworkStateUM

        /**
         * A valid address resolved to [networkUMList] (the currently selected networks). [isClickable] is `false` when
         * the address matched only a single network — there is nothing to choose, so the network-selection screen is
         * not opened.
         */
        data class Result(
            val networkUMList: ImmutableList<NetworkUM>,
            val isClickable: Boolean,
        ) : ChosenNetworkStateUM {
            data class NetworkUM(
                val networkName: String,
                @DrawableRes val iconResId: Int,
            )
        }
    }
}