package com.tangem.features.yield.supply.impl.main.model.transformers

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class YieldSupplyTokenStatusSuccessTransformerTest {

    private var startEarningClicked = false
    private var learnMoreClicked = false

    @Test
    fun `GIVEN inactive token WHEN transform THEN Unavailable`() {
        // Arrange
        val transformer = createTransformer(tokenStatus = marketToken(isActive = false))

        // Act
        val result = transformer.transform(YieldSupplyUM.Initial)

        // Assert
        assertThat(result).isEqualTo(YieldSupplyUM.Unavailable)
    }

    @Test
    fun `GIVEN active token without boost WHEN transform THEN Available with plain apy text`() {
        // Arrange
        val transformer = createTransformer(tokenStatus = marketToken(isActive = true, apy = BigDecimal("5.5")))

        // Act
        val result = transformer.transform(YieldSupplyUM.Initial)

        // Assert
        assertThat(result).isInstanceOf(YieldSupplyUM.Available::class.java)
        val available = result as YieldSupplyUM.Available
        assertThat(available.isBoostAvailable).isFalse()
        assertThat(available.apy).isEqualTo("5.5")
        assertThat(available.title).isEqualTo(
            resourceReference(R.string.yield_module_token_details_earn_notification_earning_on_your_balance_title),
        )
        assertThat(available.apyText).isEqualTo(
            combinedReference(
                resourceReference(R.string.yield_module_token_details_earn_notification_apy),
                stringReference(" 5.5%"),
            ),
        )
    }

    @Test
    fun `GIVEN active token with boost WHEN transform THEN Available with boosted apy text and title`() {
        // Arrange
        val transformer = createTransformer(
            tokenStatus = marketToken(isActive = true, apy = BigDecimal("5.5")),
            boostedApy = BigDecimal("16.5"),
        )

        // Act
        val result = transformer.transform(YieldSupplyUM.Initial)

        // Assert
        assertThat(result).isInstanceOf(YieldSupplyUM.Available::class.java)
        val available = result as YieldSupplyUM.Available
        assertThat(available.isBoostAvailable).isTrue()
        assertThat(available.title).isEqualTo(resourceReference(R.string.yield_apy_boost_banner_title))
        assertThat(available.apyText).isEqualTo(
            annotatedReference(
                buildAnnotatedString {
                    append("APY ")
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append("5.5%")
                    }
                    append(" x3 → 16.5%")
                },
            ),
        )
    }

    @Test
    fun `GIVEN active token WHEN clicks delegated THEN original callbacks fire`() {
        // Arrange
        val transformer = createTransformer(tokenStatus = marketToken(isActive = true))

        // Act
        val available = transformer.transform(YieldSupplyUM.Initial) as YieldSupplyUM.Available
        available.onClick()
        available.onLearnMoreClick()

        // Assert
        assertThat(startEarningClicked).isTrue()
        assertThat(learnMoreClicked).isTrue()
    }

    private fun createTransformer(
        tokenStatus: YieldMarketToken,
        boostedApy: BigDecimal? = null,
    ): YieldSupplyTokenStatusSuccessTransformer = YieldSupplyTokenStatusSuccessTransformer(
        tokenStatus = tokenStatus,
        onStartEarningClick = { startEarningClicked = true },
        onLearnMoreClick = { learnMoreClicked = true },
        boostedApy = boostedApy,
    )

    private fun marketToken(isActive: Boolean, apy: BigDecimal = BigDecimal("5.5")): YieldMarketToken =
        YieldMarketToken(
            tokenAddress = "0xToken",
            chainId = 1,
            apy = apy,
            isActive = isActive,
            maxFeeNative = BigDecimal.ZERO,
            maxFeeUSD = BigDecimal.ZERO,
        )
}