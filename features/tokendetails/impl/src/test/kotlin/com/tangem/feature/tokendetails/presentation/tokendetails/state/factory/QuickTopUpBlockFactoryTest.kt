package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class QuickTopUpBlockFactoryTest {

    private val factory = QuickTopUpBlockFactory()

    private val zeroBalanceStatus: CryptoCurrencyStatus = mockk {
        every { value } returns mockk<CryptoCurrencyStatus.Loaded> {
            every { amount } returns BigDecimal.ZERO
        }
    }

    private val nonZeroBalanceStatus: CryptoCurrencyStatus = mockk {
        every { value } returns mockk<CryptoCurrencyStatus.Loaded> {
            every { amount } returns BigDecimal.TEN
        }
    }

    private val usdCurrency = OnrampCurrency(name = "US Dollar", code = "USD", image = null, precision = 2, unit = "$")
    private val eurCurrency = OnrampCurrency(name = "Euro", code = "EUR", image = null, precision = 2, unit = "€")
    private val plnCurrency = OnrampCurrency(name = "Polish Zloty", code = "PLN", image = null, precision = 2, unit = "zł")

    private val usdAppCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
    private val eurAppCurrency = AppCurrency(code = "EUR", name = "Euro", symbol = "€")
    private val plnAppCurrency = AppCurrency(code = "PLN", name = "Polish Zloty", symbol = "zł")

    private val countryMock: OnrampCountry = mockk(relaxed = true)

    private val available: OnrampAvailability = OnrampAvailability.Available(country = countryMock, currency = usdCurrency)
    private val notSupported: OnrampAvailability = OnrampAvailability.NotSupported(country = countryMock)

    @Test
    fun `GIVEN non-zero balance WHEN build THEN returns null`() {
        // Act
        val result = factory.build(
            currencyStatus = nonZeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = available.right(),
            selectedOnrampCurrency = usdCurrency,
            appCurrency = usdAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN loading balance WHEN build THEN returns null`() {
        // Arrange
        val loadingStatus: CryptoCurrencyStatus = mockk {
            every { value } returns CryptoCurrencyStatus.Loading
        }

        // Act
        val result = factory.build(
            currencyStatus = loadingStatus,
            isHistoryEmpty = true,
            onrampAvailability = available.right(),
            selectedOnrampCurrency = usdCurrency,
            appCurrency = usdAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN non-empty history WHEN build THEN returns null`() {
        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = false,
            onrampAvailability = available.right(),
            selectedOnrampCurrency = usdCurrency,
            appCurrency = usdAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN onramp not available WHEN build THEN returns null`() {
        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = notSupported.right(),
            selectedOnrampCurrency = usdCurrency,
            appCurrency = usdAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN onramp availability error WHEN build THEN returns null`() {
        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = OnrampError.DataError(code = "error", description = null).left(),
            selectedOnrampCurrency = usdCurrency,
            appCurrency = usdAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN selected onramp currency USD WHEN build THEN returns block with USD presets`() {
        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = available.right(),
            selectedOnrampCurrency = usdCurrency,
            appCurrency = plnAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNotNull()
        val amounts = result!!.amounts
        assertThat(amounts.map { it.displayValue }).containsExactly(
            stringReference("$50"),
            stringReference("$200"),
            stringReference("$700"),
            resourceReference(R.string.quick_top_up_chip_other),
        ).inOrder()
        assertThat(amounts.last().isOther).isTrue()
        assertThat(amounts.take(3).all { !it.isOther }).isTrue()
    }

    @Test
    fun `GIVEN selected onramp currency EUR WHEN build THEN returns block with EUR presets`() {
        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = available.right(),
            selectedOnrampCurrency = eurCurrency,
            appCurrency = plnAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNotNull()
        val amounts = result!!.amounts
        assertThat(amounts.map { it.displayValue }).containsExactly(
            stringReference("€50"),
            stringReference("€200"),
            stringReference("€650"),
            resourceReference(R.string.quick_top_up_chip_other),
        ).inOrder()
        assertThat(amounts.last().isOther).isTrue()
    }

    @Test
    fun `GIVEN no onramp currency and EUR app currency WHEN build THEN returns block with EUR presets`() {
        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = available.right(),
            selectedOnrampCurrency = null,
            appCurrency = eurAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNotNull()
        val amounts = result!!.amounts
        assertThat(amounts.map { it.displayValue }).containsExactly(
            stringReference("€50"),
            stringReference("€200"),
            stringReference("€650"),
            resourceReference(R.string.quick_top_up_chip_other),
        ).inOrder()
    }

    @Test
    fun `GIVEN no onramp currency and USD app currency WHEN build THEN returns block with USD presets`() {
        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = available.right(),
            selectedOnrampCurrency = null,
            appCurrency = usdAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNotNull()
        assertThat(result!!.amounts.map { it.displayValue }).containsExactly(
            stringReference("$50"),
            stringReference("$200"),
            stringReference("$700"),
            resourceReference(R.string.quick_top_up_chip_other),
        ).inOrder()
    }

    @Test
    fun `GIVEN no onramp currency and PLN app currency WHEN build THEN returns null`() {
        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = available.right(),
            selectedOnrampCurrency = null,
            appCurrency = plnAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN selected PLN onramp currency and EUR app currency WHEN build THEN returns null`() {
        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = available.right(),
            selectedOnrampCurrency = plnCurrency,
            appCurrency = eurAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN ConfirmResidency with onramp-available country WHEN build THEN returns block using app currency`() {
        // Arrange
        val country = mockk<OnrampCountry>(relaxed = true) {
            every { onrampAvailable } returns true
        }
        val confirmResidency = OnrampAvailability.ConfirmResidency(country = country)

        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = confirmResidency.right(),
            selectedOnrampCurrency = null,
            appCurrency = usdAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNotNull()
        assertThat(result!!.amounts.map { it.displayValue }).containsExactly(
            stringReference("$50"),
            stringReference("$200"),
            stringReference("$700"),
            resourceReference(R.string.quick_top_up_chip_other),
        ).inOrder()
    }

    @Test
    fun `GIVEN ConfirmResidency with onramp-unavailable country WHEN build THEN returns null`() {
        // Arrange
        val country = mockk<OnrampCountry>(relaxed = true) {
            every { onrampAvailable } returns false
        }
        val confirmResidency = OnrampAvailability.ConfirmResidency(country = country)

        // Act
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = confirmResidency.right(),
            selectedOnrampCurrency = null,
            appCurrency = usdAppCurrency,
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN selected USD onramp currency WHEN preset clicked THEN callback receives resolved USD code`() {
        // Arrange
        var clickedAmount: BigDecimal? = null
        var clickedCode: String? = null
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isHistoryEmpty = true,
            onrampAvailability = available.right(),
            selectedOnrampCurrency = usdCurrency,
            appCurrency = eurAppCurrency,
            onPresetClick = { amount, code -> clickedAmount = amount; clickedCode = code },
            onOtherClick = {},
        )

        // Act
        result!!.amounts.first().onClick()

        // Assert
        assertThat(clickedCode).isEqualTo("USD")
        assertThat(clickedAmount).isEqualTo(BigDecimal(50))
    }
}