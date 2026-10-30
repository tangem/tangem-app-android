package com.tangem.grow.datasource.yield

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.grow.datasource.yield.models.YieldMarketsResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldAprHistoryPointDto
import com.tangem.grow.datasource.yield.models.incentives.YieldAprHistoryResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldIncentivesResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The `yield-modules` routes answer with their payload bare, exactly like the plain `yield` routes — the
 * `{success, timestamp, result}` envelope the contract documents does not exist on the wire. Reading a
 * bare payload through an envelope leaves every field absent, which the repository cannot tell apart from
 * a genuinely empty response.
 */
internal class YieldIncentivesDtoTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .add(BigDecimalAdapter())
        .build()

    @Test
    fun `GIVEN bare markets json WHEN parsed THEN campaign fields survive`() {
        // Arrange
        val adapter = moshi.adapter(YieldMarketsResponse::class.java)
        val json = """
            {"tokens":[{"tokenAddress":"0xabc","tokenSymbol":"USDC","tokenName":"USD Coin","apy":4,
            "supplyCap":"1000","isActive":true,"decimals":6,"chainId":1,"chain":"ethereum",
            "maxFeeNative":"0.01","maxFeeUSD":"30","isForEarn":true,
            "hasActiveCampaign":true,"grossRewardsApy":1.5,"netRewardsApy":1.275,"grossTotalApyEst":5.5,
            "netTotalApyEst":5.275}],
            "lastUpdatedAt":"2026-09-20T10:30:00.000Z"}
        """.trimIndent()

        // Act
        val parsed = adapter.fromJson(json)!!

        // Assert
        val token = parsed.marketDtos.single()
        assertThat(token.tokenAddress).isEqualTo("0xabc")
        assertThat(token.apy).isEqualTo(BigDecimal("4"))
        assertThat(token.hasActiveCampaign).isTrue()
        assertThat(token.grossRewardsApy).isEqualTo(BigDecimal("1.5"))
        assertThat(token.grossTotalApyEst).isEqualTo(BigDecimal("5.5"))
    }

    @Test
    fun `GIVEN bare incentives json WHEN parsed THEN reward fields survive`() {
        // Arrange
        val adapter = moshi.adapter(YieldIncentivesResponse::class.java)
        val json = """
            {"chainId":143,"userAddress":"0xuser","moduleAddress":"0xmodule","claimRecommended":true,
            "rewards":[{"source":"merkl","protocol":"aave","rewardToken":"0xreward","symbol":"MRK",
            "tokenDecimals":18,"claimableAmount":"1000000000000000000","pendingAmount":"0",
            "usdValue":12.5,"serviceFeeRateBps":500,"estimatedServiceFeeUsd":0.62,
            "estimatedNetValueUsd":11.88,"finalDestination":"owner"}],
            "lastUpdatedAt":"2026-09-20T10:30:00.000Z"}
        """.trimIndent()

        // Act
        val parsed = adapter.fromJson(json)!!

        // Assert
        assertThat(parsed.chainId).isEqualTo(143)
        assertThat(parsed.moduleAddress).isEqualTo("0xmodule")
        assertThat(parsed.isClaimRecommended).isTrue()
        val reward = parsed.rewards!!.single()
        assertThat(reward.rewardToken).isEqualTo("0xreward")
        assertThat(reward.claimableAmount).isEqualTo("1000000000000000000")
        assertThat(reward.serviceFeeRateBps).isEqualTo(500)
        assertThat(reward.finalDestination).isEqualTo("owner")
    }

    @Test
    fun `GIVEN enveloped incentives json WHEN parsed THEN payload is not read`() {
        // Arrange
        val adapter = moshi.adapter(YieldIncentivesResponse::class.java)
        val json = """
            {"success":true,"timestamp":"2026-09-20T10:30:00.000Z",
            "result":{"chainId":143,"moduleAddress":"0xmodule"}}
        """.trimIndent()

        // Act
        val parsed = adapter.fromJson(json)!!

        // Assert
        assertThat(parsed.chainId).isNull()
        assertThat(parsed.moduleAddress).isNull()
    }

    @Test
    fun `GIVEN bare markets json without campaign WHEN parsed THEN the plain route is unaffected`() {
        // Arrange
        val adapter = moshi.adapter(YieldMarketsResponse::class.java)
        val json = """
            {"tokens":[{"tokenAddress":"0xabc","apy":4,"isActive":true,"chainId":1,
            "maxFeeNative":"0.01","maxFeeUSD":"30"}],"lastUpdatedAt":"2026-09-20T10:30:00.000Z"}
        """.trimIndent()

        // Act
        val parsed = adapter.fromJson(json)!!

        // Assert
        assertThat(parsed.marketDtos.single().tokenAddress).isEqualTo("0xabc")
        assertThat(parsed.marketDtos.single().hasActiveCampaign).isNull()
    }

    @Test
    fun `GIVEN apr history json WHEN parsed THEN gross rate fields survive`() {
        // Arrange
        val adapter = moshi.adapter(YieldAprHistoryResponse::class.java)
        val json = """
            {"chainId":143,"tokenAddress":"0xabc","points":[{"date":"2026-10-13","baseApy":4.03,
            "grossRewardsApy":6.36,"netRewardsApy":5.4,"grossTotalApyEst":10.39,"netTotalApyEst":9.43,
            "carriedForward":false}]}
        """.trimIndent()

        // Act
        val point = adapter.fromJson(json)!!.points!!.single()

        // Assert
        assertThat(point).isEqualTo(
            YieldAprHistoryPointDto(
                date = "2026-10-13",
                baseApy = BigDecimal("4.03"),
                grossRewardsApy = BigDecimal("6.36"),
                grossTotalApyEst = BigDecimal("10.39"),
                isCarriedForward = false,
            ),
        )
    }

    private class BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String) = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal) = value.toString()
    }
}