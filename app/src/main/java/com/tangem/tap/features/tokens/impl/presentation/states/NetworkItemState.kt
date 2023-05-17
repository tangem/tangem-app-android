package com.tangem.tap.features.tokens.impl.presentation.states

import androidx.compose.runtime.MutableState
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.tap.common.extensions.getGreyedOutIconRes

/**
 * Network item state
 *
[REDACTED_AUTHOR]
 */
sealed interface NetworkItemState {

    /** Network name */
    val name: String

    /** Network protocol name */
    val protocolName: String

    /** Network icon id from resources */
    val iconResId: MutableState<Int>

    /** Flag that determines if the network is the main network for the token */
    val isMainNetwork: Boolean

    /**
     * Network item state that is available for read
     *
     * @property name          network name
     * @property protocolName  network protocol name
     * @property iconResId     network icon id from resources
     * @property isMainNetwork flag that determines if the network is the main network for the token
     */
    data class ReadContent(
        override val name: String,
        override val protocolName: String,
        override val iconResId: MutableState<Int>,
        override val isMainNetwork: Boolean,
    ) : NetworkItemState

    /**
     * Network item state that is available for read and edit
     *
     * @property name           network name
     * @property protocolName   network protocol name
     * @property iconResId      network icon id from resources
     * @property isMainNetwork  flag that determines if the network is the main network for the token
     * @property isAdded        flag that determines if the user has saved the token
     * @property id             network id
     * @property address        contract address
     * @property decimalCount   decimal count
     * @property blockchain     blockchain
     * @property onToggleClick  lambda be invoked when switch is been toggled
     * @property onNetworkClick lambda be invoked when network item is been clicked
     */
    data class ManageContent(
        override val name: String,
        override val protocolName: String,
        override val iconResId: MutableState<Int>,
        override val isMainNetwork: Boolean,
        val isAdded: MutableState<Boolean>,
        val id: String,
        val address: String?,
        val decimalCount: Int?,
        val blockchain: Blockchain,
        val onToggleClick: (TokenItemState.ManageContent, ManageContent) -> Unit,
        val onNetworkClick: () -> Unit,
    ) : NetworkItemState {

        /**
         * Change toggle state [isAdded].
         *
         * It is a hack that helps us to change element of flow
         */
        fun changeToggleState() {
            val reverseState = !isAdded.value
            isAdded.value = reverseState
            iconResId.value = if (reverseState) getActiveIconRes(blockchain.id) else blockchain.getGreyedOutIconRes()
        }
    }
}