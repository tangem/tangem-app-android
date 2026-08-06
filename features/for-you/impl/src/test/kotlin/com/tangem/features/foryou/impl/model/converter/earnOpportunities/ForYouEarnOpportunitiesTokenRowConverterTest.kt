package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.utils.StringsSigns
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouEarnOpportunitiesTokenRowConverterTest {

    private val appCurrency: AppCurrency = AppCurrency.Default
    private val converter = createConverter()

    private fun createConverter(
        userWalletId: UserWalletId? = UserWalletId("01"),
        onTokenClick: (UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType) -> Unit = { _, _, _ -> },
        isBalanceHidden: Boolean = false,
    ) = ForYouEarnOpportunitiesTokenRowConverter(
        appCurrency = appCurrency,
        userWalletId = userWalletId,
        onTokenClick = onTokenClick,
        isBalanceHidden = isBalanceHidden,
    )

    @Test
    fun `GIVEN loading status WHEN convert THEN row is Loading with currency id`() {
        // Arrange
        val status = createStatus(createEarnCurrency(currencyId = "coin-eth"), CryptoCurrencyStatus.Loading)

        // Act
        val result = converter.convert(status to createEarnApyInfo())

        // Assert
        assertThat(result).isEqualTo(TangemTokenRowUM.Loading(id = "coin-eth"))
    }

    @Test
    fun `GIVEN loaded status WHEN convert THEN top end is the yearly earn from balance and rate`() {
        // Arrange — 200 fiat at 5% → +10.00/year
        val status = createStatus(
            createEarnCurrency(currencyId = "coin-eth"),
            createRowLoadedValue(fiatAmount = BigDecimal("200")),
        )

        // Act
        val result = converter.convert(status to createEarnApyInfo(apy = BigDecimal("0.05")))
            as TangemTokenRowUM.Content

        // Assert — mirrors the production "+<fiat>/year" rendering
        val expectedEarn = BigDecimal("200").multiply(BigDecimal("0.05")).format {
            fiat(fiatCurrencySymbol = appCurrency.symbol, fiatCurrencyCode = appCurrency.code)
        }
        val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
        assertThat(topEnd.text).isEqualTo(
            combinedReference(
                stringReference(StringsSigns.PLUS + StringsSigns.WHITE_SPACE),
                resourceReference(R.string.for_you_earn_per_year, wrappedList(expectedEarn)),
            ),
        )
    }

    @Test
    fun `GIVEN loaded status WHEN convert THEN bottom end is the styled percent rate`() {
        // Arrange
        val status = createStatus(createEarnCurrency(), createRowLoadedValue())

        // Act
        val result = converter.convert(status to createEarnApyInfo(apy = BigDecimal("0.05")))
            as TangemTokenRowUM.Content

        // Assert
        val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
        val styled = bottomEnd.text as TextReference.StyledStr
        assertThat(styled.value).isEqualTo("APY " + BigDecimal("0.05").format { percent() })
    }

    @Test
    fun `GIVEN loaded status from stale cache WHEN convert THEN error-sync icon shown on both ends`() {
        // Arrange
        val status = createStatus(
            createEarnCurrency(),
            createRowLoadedValue(source = StatusSource.ONLY_CACHE),
        )

        // Act
        val result = converter.convert(status to createEarnApyInfo()) as TangemTokenRowUM.Content

        // Assert
        val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
        val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
        assertThat(topEnd.startIcons).hasSize(1)
        assertThat(bottomEnd.startIcons).hasSize(1)
    }

    @Test
    fun `GIVEN row clicked WHEN convert THEN callback receives wallet currency and resolved earn type`() {
        // Arrange
        val currency = createEarnCurrency()
        val status = createStatus(currency, createRowLoadedValue())
        val earnType = ForYouEarnOpportunitiesType.YieldSupply(apy = "5.5")
        val walletId = UserWalletId("01")
        var clicked: Triple<UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType>? = null
        val converter = createConverter(
            userWalletId = walletId,
            onTokenClick = { id, clickedCurrency, type -> clicked = Triple(id, clickedCurrency, type) },
        )

        // Act
        val result = converter.convert(status to createEarnApyInfo(type = earnType)) as TangemTokenRowUM.Content
        result.onItemClick?.invoke()

        // Assert
        assertThat(clicked).isEqualTo(Triple(walletId, currency, earnType))
    }

    @Test
    fun `GIVEN balance hidden WHEN convert THEN yearly earn masked but percent rate visible`() {
        // Arrange — 200 fiat at 5%
        val status = createStatus(createEarnCurrency(), createRowLoadedValue(fiatAmount = BigDecimal("200")))
        val converter = createConverter(isBalanceHidden = true)

        // Act
        val result = converter.convert(status to createEarnApyInfo(apy = BigDecimal("0.05")))
            as TangemTokenRowUM.Content

        // Assert — the projected fiat earn is masked, the styled APY percent stays visible
        val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
        val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
        assertThat(topEnd.text).isEqualTo(
            combinedReference(
                TextReference.EMPTY,
                resourceReference(R.string.for_you_earn_per_year, wrappedList(StringsSigns.THREE_STARS)),
            ),
        )
        val styled = bottomEnd.text as TextReference.StyledStr
        assertThat(styled.value).isEqualTo("APY " + BigDecimal("0.05").format { percent() })
    }

    @Test
    fun `GIVEN unreachable status WHEN convert THEN both ends are dashes`() {
        // Arrange
        val unreachable: CryptoCurrencyStatus.Unreachable = mockk {
            every { fiatAmount } returns null
            every { isError } returns true
        }
        val status = createStatus(createEarnCurrency(), unreachable)

        // Act
        val result = converter.convert(status to createEarnApyInfo()) as TangemTokenRowUM.Content

        // Assert
        val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
        val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
        assertThat(topEnd.text).isEqualTo(stringReference(StringsSigns.DASH_SIGN))
        assertThat(bottomEnd.text).isEqualTo(stringReference(StringsSigns.DASH_SIGN))
    }
}