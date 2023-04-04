package com.tangem.tap.features.tokens.presentation.states

/**
 * Network item state
 *
[REDACTED_AUTHOR]
 */
sealed interface AddTokensNetworkItemState {

    /** Network name */
    val name: String

    /** Network protocol name */
    val protocolName: String

    /** Network icon id from resources */
    val iconResId: Int

    /** Flag that determines if the network is the main network for the token */
    val isMainNetwork: Boolean

    /**
     * Network item state that is available for read
     *
     * @property name          network name
     * @property iconResId     network icon id from resources
     * @property isMainNetwork flag that determines if the network is the main network for the token
     */
    data class ReadAccess(
        override val name: String,
        override val protocolName: String,
        override val iconResId: Int,
        override val isMainNetwork: Boolean,
    ) : AddTokensNetworkItemState

    /**
     * Network item state that is available for read and edit
     *
     * @property name          network name
     * @property iconResId     network icon id from resources
     * @property isMainNetwork flag that determines if the network is the main network for the token
     * @property isAdded       flag that determines if the user has saved the token
     * @property networkId     network id
     * @property onToggleClick lambda be invoked when switch is been toggled
     */
    data class EditAccess(
        override val name: String,
        override val protocolName: String,
        override val iconResId: Int,
        override val isMainNetwork: Boolean,
        val isAdded: Boolean,
        val networkId: String,
        val onToggleClick: (String) -> Unit,
    ) : AddTokensNetworkItemState
}