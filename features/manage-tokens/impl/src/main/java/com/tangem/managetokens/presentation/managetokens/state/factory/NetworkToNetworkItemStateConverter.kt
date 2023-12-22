package com.tangem.managetokens.presentation.managetokens.state.factory

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.tangem.core.ui.extensions.getActiveIconResByNetworkId
import com.tangem.core.ui.extensions.getGreyedOutIconResByNetworkId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.common.utils.CurrencyUtils
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class NetworkToNetworkItemStateConverter(
    private val addedCurrenciesByWalletProvider: Provider<MutableMap<UserWallet, MutableList<CryptoCurrency>>>,
    private val selectedWalletProvider: Provider<UserWallet?>,
    private val onNetworkToggleClick: (token: TokenItemState.Loaded, network: NetworkItemState.Toggleable) -> Unit,
) : Converter<Token.Network, NetworkItemState> {

    override fun convert(value: Token.Network): NetworkItemState {
        return createManageNetworkContent(value)
    }

    private fun createManageNetworkContent(network: Token.Network): NetworkItemState {
        val addedCurrencies = addedCurrenciesByWalletProvider()[selectedWalletProvider()] ?: emptyList()
        val isAdded = CurrencyUtils.isAdded(
            address = network.address,
            networkId = network.networkId,
            currencies = addedCurrencies,
        )
        return NetworkItemState.Toggleable(
            name = network.name,
            iconResId = mutableIntStateOf(
                getNetworkIconResId(isAdded, network.networkId), // todo
            ),
            isMainNetwork = isMainNetwork(network),
            isAdded = mutableStateOf(isAdded),
            id = network.networkId,
            protocolName = network.standardType,
            address = network.address,
            decimals = network.decimalCount,
            onToggleClick = onNetworkToggleClick,
        )
    }

    private fun getNetworkIconResId(isAdded: Boolean, networkId: String): Int {
        return if (isAdded) {
            getActiveIconResByNetworkId(networkId)
        } else {
            getGreyedOutIconResByNetworkId(networkId)
        }
    }

    private fun isMainNetwork(network: Token.Network) = network.address == null
}