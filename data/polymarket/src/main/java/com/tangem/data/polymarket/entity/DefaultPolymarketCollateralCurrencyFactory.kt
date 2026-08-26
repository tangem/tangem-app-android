package com.tangem.data.polymarket.entity

import com.tangem.blockchainsdk.utils.getSupportedTransactionExtras
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.data.common.currency.getTokenId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PolymarketDepositBlockchain
import com.tangem.domain.polymarket.approval.PolymarketContracts
import javax.inject.Inject
import javax.inject.Singleton

/**
 * No key is ever derived for the collateral and a user needs nothing in their portfolio to trade, so its network
 * is described from the chain alone rather than through the wallet-bound factory the portfolio's currencies are
 * built by.
 */
@Singleton
internal class DefaultPolymarketCollateralCurrencyFactory @Inject constructor() : PolymarketCollateralCurrencyFactory {

    private val network by lazy(mode = LazyThreadSafetyMode.NONE) {
        val blockchain = PolymarketDepositBlockchain

        Network(
            id = Network.ID(value = blockchain.toNetworkId(), derivationPath = Network.DerivationPath.None),
            name = blockchain.fullName,
            currencySymbol = blockchain.currency,
            derivationPath = Network.DerivationPath.None,
            isTestnet = blockchain.isTestnet(),
            standardType = Network.StandardType.Unspecified(blockchain.name),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = blockchain.getSupportedTransactionExtras(),
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    override fun create(): CryptoCurrency.Token {
        return CryptoCurrency.Token(
            id = getTokenId(
                network = network,
                rawTokenId = PolymarketCollateralCurrencyFactory.TOKEN_ID,
                contractAddress = PolymarketContracts.COLLATERAL,
            ),
            network = network,
            name = PolymarketCollateralCurrencyFactory.TOKEN_NAME,
            symbol = PolymarketCollateralCurrencyFactory.TOKEN_SYMBOL,
            decimals = PolymarketContracts.COLLATERAL_DECIMALS,
            iconUrl = getTokenIconUrlFromDefaultHost(PolymarketCollateralCurrencyFactory.TOKEN_ID),
            isCustom = false,
            contractAddress = PolymarketContracts.COLLATERAL,
        )
    }
}