package com.tangem.domain.marketing

import arrow.core.Either
import com.tangem.domain.marketing.models.MarketingCampaign
import com.tangem.domain.marketing.models.MarketingCampaignTarget
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.marketing.models.MarketingScreenType
import java.math.BigDecimal

class GetMarketingBannerUseCase(
    private val repository: MarketingRepository,
    private val featureToggles: MarketingFeatureToggles,
) {

    /**
     * Returns campaigns for [screen], filtered (dismissed, target match, USD amount range) and sorted by priority.
     *
     * @param amountUsd USD equivalent of the entered amount (swap/onramp only). When null, the amount filter is skipped.
     */
    suspend operator fun invoke(
        screen: MarketingScreen,
        amountUsd: BigDecimal? = null,
    ): Either<Throwable, List<MarketingCampaign>> {
        if (!featureToggles.isMarketingBannersEnabled) return Either.Right(emptyList())

        return repository.getCampaigns(screen).map { campaigns ->
            val dismissed = repository.getDismissedBannerIds()
            campaigns.asSequence()
                .filterNot { it.id in dismissed }
                .filter { matchesTarget(it, screen) }
                .filter { matchesAmount(it, amountUsd) }
                .sortedBy { it.priority }
                .toList()
        }
    }

    private fun matchesTarget(campaign: MarketingCampaign, screen: MarketingScreen): Boolean = when (screen) {
        is MarketingScreen.Swap, is MarketingScreen.Onramp -> true // matched server-side by pair params
        is MarketingScreen.TokenDetails -> matchesNetworkContract(campaign, screen.networkId, screen.contractAddress)
        is MarketingScreen.Staking -> matchesNetworkContract(campaign, screen.networkId, screen.contractAddress)
        is MarketingScreen.Yield -> matchesNetworkContract(campaign, screen.networkId, screen.contractAddress)
        is MarketingScreen.TokenMarkets -> campaign.targets.any { target ->
            target is MarketingCampaignTarget.CoingeckoId && target.id == screen.coingeckoId
        }
    }

    private fun matchesNetworkContract(
        campaign: MarketingCampaign,
        networkId: String,
        contractAddress: String,
    ): Boolean {
        return campaign.targets.any { target ->
            target is MarketingCampaignTarget.NetworkContract &&
                target.networkId == networkId &&
                contractAddressMatches(target = target.contractAddress, screen = contractAddress)
        }
    }

    /**
     * Native coins have no contract address: the backend sends `contractAddress: null` and the screen
     * passes an empty string, so blank/null on both sides is a native-coin match. Otherwise the
     * contracts must match case-insensitively.
     */
    private fun contractAddressMatches(target: String?, screen: String): Boolean {
        val normalizedTarget = target?.takeIf { it.isNotBlank() }
        val normalizedScreen = screen.takeIf { it.isNotBlank() }
        return when {
            normalizedTarget == null && normalizedScreen == null -> true
            normalizedTarget != null && normalizedScreen != null ->
                normalizedTarget.equals(normalizedScreen, ignoreCase = true)
            else -> false
        }
    }

    private fun matchesAmount(campaign: MarketingCampaign, amountUsd: BigDecimal?): Boolean {
        val isAmountScreen = campaign.type == MarketingScreenType.SWAP || campaign.type == MarketingScreenType.ONRAMP
        if (!isAmountScreen || amountUsd == null) return true

        val min = campaign.minAmount
        val max = campaign.maxAmount
        if (min != null && amountUsd < min) return false
        if (max != null && amountUsd > max) return false
        return true
    }
}