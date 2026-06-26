package com.tangem.domain.marketing.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class MarketingScreenTypeTest {

    @Test
    fun `GIVEN screen types WHEN read value THEN matches backend snake_case contract`() {
        // Assert
        assertThat(MarketingScreenType.SWAP.value).isEqualTo("swap")
        assertThat(MarketingScreenType.ONRAMP.value).isEqualTo("onramp")
        assertThat(MarketingScreenType.TOKEN_DETAILS.value).isEqualTo("token_details")
        assertThat(MarketingScreenType.TOKEN_MARKETS.value).isEqualTo("token_markets")
        assertThat(MarketingScreenType.STAKING.value).isEqualTo("staking")
        assertThat(MarketingScreenType.YIELD.value).isEqualTo("yield")
    }

    @Test
    fun `GIVEN known value WHEN fromValue THEN returns type ELSE null`() {
        // Assert
        assertThat(MarketingScreenType.fromValue("token_details")).isEqualTo(MarketingScreenType.TOKEN_DETAILS)
        assertThat(MarketingScreenType.fromValue("unknown")).isNull()
    }

    @Test
    fun `GIVEN screen type WHEN isCacheable THEN only background types cached`() {
        // Assert
        assertThat(MarketingScreenType.SWAP.isCacheable).isFalse()
        assertThat(MarketingScreenType.ONRAMP.isCacheable).isFalse()
        assertThat(MarketingScreenType.TOKEN_DETAILS.isCacheable).isTrue()
        assertThat(MarketingScreenType.TOKEN_MARKETS.isCacheable).isTrue()
        assertThat(MarketingScreenType.STAKING.isCacheable).isTrue()
        assertThat(MarketingScreenType.YIELD.isCacheable).isTrue()
    }
}