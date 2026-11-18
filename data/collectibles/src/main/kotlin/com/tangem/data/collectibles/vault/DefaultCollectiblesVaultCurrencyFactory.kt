package com.tangem.data.collectibles.vault

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.getSupportedTransactionExtras
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.data.common.currency.getTokenId
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencies
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.collectibles.account.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import javax.inject.Inject

/**
 * The vault's token is described through the collectibles owner key path rather than the wallet-bound factory the
 * portfolio's currencies are built by, so it never resolves to the wallet's ordinary Solana network.
 */
internal class DefaultCollectiblesVaultCurrencyFactory @Inject constructor() : CollectiblesVaultCurrencyFactory {

    private val network by lazy(mode = LazyThreadSafetyMode.NONE) {
        val blockchain = Blockchain.Solana
        val derivationPath = Network.DerivationPath.Custom(COLLECTIBLES_OWNER_DERIVATION_PATH)

        Network(
            id = Network.ID(value = blockchain.toNetworkId(), derivationPath = derivationPath),
            name = blockchain.fullName,
            currencySymbol = blockchain.currency,
            derivationPath = derivationPath,
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