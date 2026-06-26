package com.tangem.domain.marketing.models

/**
 * Screen-specific request context. swap/onramp carry the pair params sent to the backend; background types
 * carry the on-screen token identity used for client-side target matching (the request itself sends only [type]).
 */
sealed interface MarketingScreen {

    val type: MarketingScreenType

    data class Swap(
        val fromNetwork: String,
        val fromContractAddress: String,
        val toNetwork: String,
        val toContractAddress: String,
    ) : MarketingScreen {
        override val type: MarketingScreenType = MarketingScreenType.SWAP
    }

    data class Onramp(
        val fromFiat: String,
        val toNetwork: String,
        val toToken: String,
    ) : MarketingScreen {
        override val type: MarketingScreenType = MarketingScreenType.ONRAMP
    }

    data class TokenDetails(val networkId: String, val contractAddress: String) : MarketingScreen {
        override val type: MarketingScreenType = MarketingScreenType.TOKEN_DETAILS
    }

    data class TokenMarkets(val coingeckoId: String) : MarketingScreen {
        override val type: MarketingScreenType = MarketingScreenType.TOKEN_MARKETS
    }

    data class Staking(val networkId: String, val contractAddress: String) : MarketingScreen {
        override val type: MarketingScreenType = MarketingScreenType.STAKING
    }

    data class Yield(val networkId: String, val contractAddress: String) : MarketingScreen {
        override val type: MarketingScreenType = MarketingScreenType.YIELD
    }
}