package com.tangem.managetokens.presentation.managetokens.state.factory

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.getGreyedOutIconRes
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.managetokens.presentation.common.state.NetworkItemState
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
        val blockchain = requireNotNull(Blockchain.fromNetworkId(network.networkId)) {
            "Network ID must be valid"
        }
        val isAdded = isAdded(
            address = network.address,
            blockchain = blockchain,
            currencies = addedCurrencies,
        )
        return NetworkItemState.Toggleable(
            name = blockchain.fullName.uppercase(),
            iconResId = mutableIntStateOf(
                getNetworkIconResId(isAdded, blockchain.id),
            ),
            isMainNetwork = isMainNetwork(network),
            isAdded = mutableStateOf(isAdded),
            id = network.networkId,
            protocolName = network.standardType,
            address = network.address,
            decimals = network.decimalCount,
            blockchain = blockchain,
            onToggleClick = onNetworkToggleClick,
        )
    }

    private fun getNetworkIconResId(isAdded: Boolean, blockchainId: String): Int {
        return if (isAdded) {
            getActiveIconRes(blockchainId)
        } else {
            getGreyedOutIconRes(blockchainId)
        }
    }

    private fun isMainNetwork(network: Token.Network) = network.address == null

    private fun isAdded(address: String?, blockchain: Blockchain, currencies: Collection<CryptoCurrency>): Boolean {
        return if (address != null) {
            currencies.any {
                !it.isCustom && it is CryptoCurrency.Token && it.contractAddress == address
            }
        } else {
            currencies.any {
                !it.isCustom && it is CryptoCurrency.Coin && it.name == blockchain.fullName
            }
        }
    }
}