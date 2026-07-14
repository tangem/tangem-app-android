package com.tangem.features.promobanners.impl.campaigns.model

import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.promo.models.PromoPayoutToken
import com.tangem.features.commonfeatures.api.choosetoken.PredefinedTokenToAdd
import javax.inject.Inject

/**
 * Maps backend promo payout tokens into [PredefinedTokenToAdd] for the token chooser.
 *
 * The backend payload already carries everything needed — `tokenId` (the market raw id), decimals,
 * symbol, name, contract address and network id — so no catalog lookup is required. The icon url is
 * derived from the raw id via the canonical host helper, matching what the add-to-portfolio flow uses.
 */
internal class PredefinedTokenResolver @Inject constructor() {

    fun resolve(payoutTokens: List<PromoPayoutToken>): List<PredefinedTokenToAdd> = payoutTokens
        .map { payoutToken -> payoutToken.toPredefinedToken() }
        .distinctBy { it.token.id.value to it.network.networkId }

    private fun PromoPayoutToken.toPredefinedToken(): PredefinedTokenToAdd {
        val rawId = CryptoCurrency.RawID(tokenId)
        return PredefinedTokenToAdd(
            token = RawMarketToken(
                id = rawId,
                name = tokenName,
                symbol = tokenSymbol,
            ),
            network = TokenMarketInfo.Network(
                networkId = networkId,
                isExchangeable = false,
                contractAddress = tokenAddress,
                decimalCount = decimals,
            ),
            iconUrl = getTokenIconUrlFromDefaultHost(rawId),
        )
    }
}