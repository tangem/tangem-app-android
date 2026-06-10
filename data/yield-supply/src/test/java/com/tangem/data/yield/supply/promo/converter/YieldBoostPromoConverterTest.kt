package com.tangem.data.yield.supply.promo.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.promotion.models.PromotionsResponse
import com.tangem.domain.yield.supply.models.YieldBoostPromo
import org.junit.jupiter.api.Test

class YieldBoostPromoConverterTest {

    @Test
    fun `GIVEN active dto with tokens WHEN convert THEN returns Active`() {
        val dto = activeDto()

        val result = YieldBoostPromoConverter.convert(dto)

        assertThat(result).isInstanceOf(YieldBoostPromo.Active::class.java)
        val active = result as YieldBoostPromo.Active
        assertThat(active.tokens).hasSize(2)
        assertThat(active.tokens.first().contractAddress)
            .isEqualTo("0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48")
        assertThat(active.tokens.first().networkId).isEqualTo("ethereum")
        assertThat(active.link).isEqualTo("https://example.com/terms")
    }

    @Test
    fun `GIVEN dto with null all WHEN convert THEN returns None`() {
        val dto = PromotionsResponse.PromotionDto(name = "promo-yield-apr-boost", all = null)

        val result = YieldBoostPromoConverter.convert(dto)

        assertThat(result).isEqualTo(YieldBoostPromo.None)
    }

    @Test
    fun `GIVEN dto with non-active status WHEN convert THEN returns None`() {
        val dto = activeDto().copy(
            all = activeDto().all!!.copy(status = "expired"),
        )

        val result = YieldBoostPromoConverter.convert(dto)

        assertThat(result).isEqualTo(YieldBoostPromo.None)
    }

    @Test
    fun `GIVEN dto with empty tokens WHEN convert THEN returns None`() {
        val dto = activeDto().copy(
            all = activeDto().all!!.copy(tokens = emptyList()),
        )

        val result = YieldBoostPromoConverter.convert(dto)

        assertThat(result).isEqualTo(YieldBoostPromo.None)
    }

    @Test
    fun `GIVEN dto with null tokens WHEN convert THEN returns None`() {
        val dto = activeDto().copy(
            all = activeDto().all!!.copy(tokens = null),
        )

        val result = YieldBoostPromoConverter.convert(dto)

        assertThat(result).isEqualTo(YieldBoostPromo.None)
    }

    @Test
    fun `GIVEN dto with malformed start date WHEN convert THEN returns None`() {
        val dto = activeDto().copy(
            all = activeDto().all!!.copy(
                timeline = PromotionsResponse.PromotionDto.Timeline(
                    start = "not-an-iso",
                    end = "2027-06-15T22:00:00.000Z",
                ),
            ),
        )

        val result = YieldBoostPromoConverter.convert(dto)

        assertThat(result).isEqualTo(YieldBoostPromo.None)
    }

    @Test
    fun `GIVEN status with uppercase casing WHEN convert THEN treats as active`() {
        val dto = activeDto().copy(
            all = activeDto().all!!.copy(status = "ACTIVE"),
        )

        val result = YieldBoostPromoConverter.convert(dto)

        assertThat(result).isInstanceOf(YieldBoostPromo.Active::class.java)
    }

    private fun activeDto() = PromotionsResponse.PromotionDto(
        name = "promo-yield-apr-boost",
        all = PromotionsResponse.PromotionDto.All(
            timeline = PromotionsResponse.PromotionDto.Timeline(
                start = "2026-06-15T00:00:00.000Z",
                end = "2027-06-15T22:00:00.000Z",
            ),
            tokens = listOf(
                PromotionsResponse.PromotionDto.PromoToken(
                    tokenAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                    tokenSymbol = "USDC",
                    tokenName = "USD Coin",
                    networkId = "ethereum",
                ),
                PromotionsResponse.PromotionDto.PromoToken(
                    tokenAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7",
                    tokenSymbol = "USDT",
                    tokenName = "Tether USD",
                    networkId = "ethereum",
                ),
            ),
            status = "active",
            link = "https://example.com/terms",
        ),
    )
}