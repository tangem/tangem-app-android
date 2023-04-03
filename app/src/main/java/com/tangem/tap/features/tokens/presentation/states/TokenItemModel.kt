package com.tangem.tap.features.tokens.presentation.states

/**
 * Token model
 *
 * @property name     token name
 * @property iconUrl  token icon url
 * @property networks list of networks
 */
data class TokenItemModel(
    val name: String,
    val iconUrl: String,
    val networks: List<AddTokensNetworkItemState>,
)