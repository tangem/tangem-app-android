package com.tangem.features.earn.lending.impl.session

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.lending.impl.provider.LendingDepositGate
import com.tangem.features.earn.lending.impl.provider.LendingMarket
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.lending.impl.supplier.LendingAssetState
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LendingDepositRowsTest {

    private val currencies = MockCryptoCurrencyFactory()
    private val eth = currencies.createCoin(Blockchain.Ethereum)
    private val asset = LendingAsset(
        currency = eth,
        wrappedToken = currencies.createToken(
            blockchain = Blockchain.Ethereum,
            contractAddress = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2",
            symbol = "WETH",
            decimals = 18,
        ),
    )

    private val lendingProvider: LendingProvider = mockk {
        every { performanceFee } returns BigDecimal("0.15")
    }

    private val rows = LendingDepositRows(lendingProvider = lendingProvider)

    private var previousLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun pinLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun `GIVEN rate unknown WHEN of THEN no rows`() {
        // Act
        val actual = rows.of(state(rate = null), DEPOSIT)

        // Assert
        assertThat(actual).isEmpty()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN rate and fiat rate WHEN of THEN rate, fee and yearly fiat earnings after the fee`(model: EarningsModel) {
        // Act
        val actual = rows.of(state(fiatRate = model.fiatRate), DEPOSIT)

        // Assert
        val earningsRow = EarnRow(
            label = resourceReference(R.string.lending_projected_earnings),
            value = EarnValue.YearlyFiatEarnings(model.earnings),
        )
        assertThat(actual).containsExactly(apyRow, feeRow, earningsRow).inOrder()
    }

    private fun provideTestModels() = listOf(
        // 0.5 ETH × $1600 × 5.5% × (1 − 15%) = $37.40
        EarningsModel(fiatRate = BigDecimal("1600"), earnings = BigDecimal("37.40")),
        // 0.5 ETH × $20 × 5.5% × (1 − 15%) = $0.4675, rounded to cents
        EarningsModel(fiatRate = BigDecimal("20"), earnings = BigDecimal("0.47")),
        // 0.5 ETH × $20.012 = $10.006, × 5.5% × (1 − 15%) = $0.46778: only the earnings are rounded, not the total
        EarningsModel(fiatRate = BigDecimal("20.012"), earnings = BigDecimal("0.47")),
    )

    @Test
    fun `GIVEN fiat rate unknown WHEN of THEN no yearly earnings`() {
        // Act
        val actual = rows.of(state(fiatRate = null), DEPOSIT)

        // Assert
        assertThat(actual).containsExactly(apyRow, feeRow).inOrder()
    }

    data class EarningsModel(val fiatRate: BigDecimal, val earnings: BigDecimal)

    private val apyRow = EarnRow(
        label = resourceReference(R.string.yield_module_earn_sheet_current_apy_title),
        value = EarnValue.Text(stringReference("5.5%")),
    )

    private val feeRow = EarnRow(
        label = resourceReference(R.string.earn_product_details_performance_fee),
        value = EarnValue.Text(stringReference("15%")),
    )

    private fun state(rate: BigDecimal? = BigDecimal("0.055"), fiatRate: BigDecimal? = null) = LendingAssetState(
        asset = asset,
        address = null,
        balance = null,
        fiatRate = fiatRate,
        market = LendingMarket(
            rate = rate?.let { EarnProductRate(fraction = it, type = EarnRateType.APY) },
            isOpen = true,
            deposit = LendingDepositGate.Open,
            position = null,
            maxWithdraw = null,
            providerState = null,
            source = StatusSource.ACTUAL,
        ),
        hasPendingTx = false,
        isWrappedTokenInWallet = false,
        source = StatusSource.ACTUAL,
    )

    private companion object {
        val DEPOSIT = BigDecimal("0.5")
    }
}