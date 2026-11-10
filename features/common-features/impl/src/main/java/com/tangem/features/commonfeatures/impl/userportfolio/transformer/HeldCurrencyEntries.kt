package com.tangem.features.commonfeatures.impl.userportfolio.transformer

import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData

/**
 * The user's holdings of [rawCurrencyId] across every wallet and crypto portfolio account.
 *
 * @param networkIds when not `null`, only the holdings on these networks
 *   ([com.tangem.domain.markets.TokenMarketInfo.Network.networkId]) are returned
 */
internal fun AvailableToAddData.heldCurrencyEntries(
    rawCurrencyId: CryptoCurrency.RawID,
    networkIds: Set<String>?,
): List<TokenSelectorEntry> {
    return availableToAddWallets.values.flatMap { wallet ->
        wallet.accounts.filterCryptoPortfolio().flatMap { accountStatus ->
            accountStatus.tokenList.flattenCurrencies()
                .filter { status ->
                    status.currency.matchesRawId(rawCurrencyId) &&
                        (networkIds == null || status.currency.network.rawId in networkIds)
                }
                .map { status ->
                    TokenSelectorEntry(
                        wallet = wallet.userWallet,
                        account = accountStatus,
                        currencyStatus = status,
                    )
                }
        }
    }
}

private fun CryptoCurrency.matchesRawId(target: CryptoCurrency.RawID): Boolean {
    val rawId = id.rawCurrencyId ?: return false
    return getTokenIdIfL2Network(rawId.value) == target.value
}