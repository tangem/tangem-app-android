package com.tangem.managetokens.presentation.common.state

import androidx.compose.runtime.MutableState
import com.tangem.core.ui.extensions.*
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState

/**
 * Network item state
 *
 * @property name           network name
 * @property protocolName   network protocol name
 * @property id             network id
 * @property iconRes        network icon id from resources
 */
internal sealed interface NetworkItemState {

    val name: String
    val protocolName: String
    val id: String

    val iconRes: Int
        get() = when (this) {
            is Selectable -> this.iconResId
            is Toggleable -> this.iconResId.value
        }

    /**
     * Network item state that can be added and deleted
     *
     * @property name           network name
     * @property protocolName   network protocol name
     * @property id             network id
     * @property iconResId      network icon id from resources
     * @property isMainNetwork  flag that determines if the network is the main network for the token
     * @property isAdded        flag that determines if the user has saved the network
     * @property address        contract address
     * @property decimals       decimal count
     * @property onToggleClick  lambda be invoked when switch is been toggled
     */
    @Suppress("LongParameterList")
    data class Toggleable(
        override val name: String,
        override val protocolName: String,
        override val id: String,
        val iconResId: MutableState<Int>,
        val isMainNetwork: Boolean,
        val isAdded: MutableState<Boolean>,
        val address: String?,
        val decimals: Int?,
        val onToggleClick: (TokenItemState.Loaded, Toggleable) -> Unit,
    ) : NetworkItemState {

        /**
         * Change toggle state [isAdded].
         *
         * It is a hack that helps us to change element of flow
         */
        fun changeToggleState() {
            val reverseState = !isAdded.value
            isAdded.value = reverseState
            iconResId.value = if (reverseState) getActiveIconResByNetworkId(id) else getGreyedOutIconResByNetworkId(id)
        }
    }

    /**
     * Network item state that can be selected
     *
     * @property name           network name
     * @property protocolName   network protocol name
     * @property iconResId      network icon id from resources
     * @property id             network id
     * @property onNetworkClick lambda be invoked when network item is been clicked
     *
     */
    data class Selectable(
        override val name: String,
        override val protocolName: String,
        val iconResId: Int,
        override val id: String,
        val onNetworkClick: (NetworkItemState) -> Unit,
    ) : NetworkItemState
}