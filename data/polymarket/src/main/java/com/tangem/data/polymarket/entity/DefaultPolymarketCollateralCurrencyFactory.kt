package com.tangem.data.polymarket.entity

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.getSupportedTransactionExtras
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.data.common.currency.getTokenId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PolymarketDepositBlockchain
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketDepositAsset
import com.tangem.domain.polymarket.model.PredictionCollateral
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * No key is ever derived for the collateral and a user needs nothing in their portfolio to trade, so its network
 * is described from the chain alone rather than through the wallet-bound factory the portfolio's currencies are
 * built by.
 */
@Singleton
internal class DefaultPolymarketCollateralCurrencyFactory @Inject constructor() : PolymarketCollateralCurrencyFactory {

    private val networks = ConcurrentHashMap<Blockchain, Network>()

    override fun create(): CryptoCurrency.Token = token(
        network = network(PolymarketDepositBlockchain),
        contractAddress = PolymarketContracts.COLLATERAL.lowercase(),
        name = PolymarketCollateralCurrencyFactory.COLLATERAL_NAME,
        symbol = PolymarketCollateralCurrencyFactory.COLLATERAL_SYMBOL,
        decimals = PolymarketContracts.COLLATERAL_DECIMALS,
    )

    override fun createDepositTarget(asset: PolymarketDepositAsset): CryptoCurrency.Token = token(
        network = network(asset.blockchain),
        contractAddress = asset.contractAddress,
        name = PolymarketCollateralCurrencyFactory.DEPOSIT_NAME,
        symbol = PolymarketCollateralCurrencyFactory.DEPOSIT_SYMBOL,
        decimals = asset.decimals,
    )

    private fun network(blockchain: Blockchain): Network = networks.getOrPut(blockchain) {
        Network(
            id = Network.ID(value = blockchain.toNetworkId(), derivationPath = Network.DerivationPath.None),
            name = blockchain.fullName,
            currencySymbol = blockchain.currency,
            derivationPath = Network.DerivationPath.None,
            isTestnet = blockchain.isTestnet(),
            standardType = blockchain.standardType(),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = blockchain.getSupportedTransactionExtras(),
            nameResolvingType = blockchain.nameResolvingType(),
        )
    }

    private fun Blockchain.standardType(): Network.StandardType = when (this) {
        Blockchain.Ethereum -> Network.StandardType.ERC20
        Blockchain.BSC -> Network.StandardType.BEP20
        else -> Network.StandardType.Unspecified(name)
    }

    private fun Blockchain.nameResolvingType(): Network.NameResolvingType = when (this) {
        Blockchain.Ethereum -> Network.NameResolvingType.ENS
        else -> Network.NameResolvingType.NONE
    }

    private fun token(network: Network, contractAddress: String, name: String, symbol: String, decimals: Int) =
        CryptoCurrency.Token(
            id = getTokenId(
                network = network,
                rawTokenId = PredictionCollateral.RAW_ID,
                contractAddress = contractAddress,
            ),
            network = network,
            name = name,
            symbol = symbol,
            decimals = decimals,
            iconUrl = getTokenIconUrlFromDefaultHost(PredictionCollateral.RAW_ID),
            isCustom = false,
            contractAddress = contractAddress,
        )
}