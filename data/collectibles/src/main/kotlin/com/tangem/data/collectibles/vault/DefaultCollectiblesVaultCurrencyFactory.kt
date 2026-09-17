package com.tangem.data.collectibles.vault

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.getSupportedTransactionExtras
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.data.common.currency.getTokenId
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencies
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import javax.inject.Inject

/**
 * No key is derived for the vault's token and nothing about it is wallet-specific, so its network is described
 * from the chain alone rather than through the wallet-bound factory the portfolio's currencies are built by.
 */
internal class DefaultCollectiblesVaultCurrencyFactory @Inject constructor() : CollectiblesVaultCurrencyFactory {

    private val network by lazy(mode = LazyThreadSafetyMode.NONE) {
        val blockchain = Blockchain.Solana

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

    override fun create(mint: String): CryptoCurrency.Token {
        return CryptoCurrency.Token(
            id = getTokenId(
                network = network,
                rawTokenId = CollectiblesVaultCurrencies.USDC_RAW_ID,
                contractAddress = mint,
            ),
            network = network,
            name = CollectiblesVaultCurrencies.TOKEN_NAME,
            symbol = CollectiblesVaultCurrencies.TOKEN_SYMBOL,
            decimals = CollectiblesVaultCurrencies.DECIMALS,
            iconUrl = getTokenIconUrlFromDefaultHost(CollectiblesVaultCurrencies.USDC_RAW_ID),
            isCustom = false,
            contractAddress = mint,
        )
    }
}