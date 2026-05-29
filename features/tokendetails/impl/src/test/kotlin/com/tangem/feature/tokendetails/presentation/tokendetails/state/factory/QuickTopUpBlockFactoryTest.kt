package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.features.tokendetails.TokenDetailsFeatureToggles
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class QuickTopUpBlockFactoryTest {

    private val featureToggles: TokenDetailsFeatureToggles = mockk {
        every { isQuickTopUpEnabled } returns true
    }
    private val factory = QuickTopUpBlockFactory(featureToggles)

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

    private val usdCurrency = OnrampCurrency(
        name = "US Dollar",
        code = "USD",
        image = null,
        precision = 2,
        unit = "$",
    )

    private val eurCurrency = OnrampCurrency(
        name = "Euro",
        code = "EUR",
        image = null,
        precision = 2,
        unit = "€",
    )

    private val gbpCurrency = OnrampCurrency(
        name = "British Pound",
        code = "GBP",
        image = null,
        precision = 2,
        unit = "£",
    )

    private val countryMock: OnrampCountry = mockk(relaxed = true)

    private val availableUsd: OnrampAvailability = OnrampAvailability.Available(
        country = countryMock,
        currency = usdCurrency,
    )

    private val notSupported: OnrampAvailability = OnrampAvailability.NotSupported(country = countryMock)

    private val emptyHistory = TxHistoryStateError.EmptyTxHistories.left()
    private val histWithItems = 5.right()
    private val histRightZero = 0.right()

    @Test
    fun `returns null when feature toggle is disabled`() {
        val disabledToggles: TokenDetailsFeatureToggles = mockk {
            every { isQuickTopUpEnabled } returns false
        }
        val disabledFactory = QuickTopUpBlockFactory(disabledToggles)

        val result = disabledFactory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = availableUsd.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when balance is non-zero`() {
        val result = factory.build(
            currencyStatus = nonZeroBalanceStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = availableUsd.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when history has transactions`() {
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = histWithItems,
            onrampAvailability = availableUsd.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when onramp is not available`() {
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = notSupported.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when currency is not USD or EUR`() {
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = OnrampAvailability.Available(
                country = countryMock,
                currency = gbpCurrency,
            ).right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns block with USD presets when all conditions met`() {
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = availableUsd.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

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
    fun `returns block with EUR presets`() {
        val availableEur = OnrampAvailability.Available(
            country = countryMock,
            currency = eurCurrency,
        )

        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = availableEur.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

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
    fun `returns block when history count is right zero (boundary case)`() {
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = histRightZero,
            onrampAvailability = availableUsd.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNotNull()
    }

    @Test
    fun `returns block when ConfirmResidency and country supports onramp with USD`() {
        val usdCountry = OnrampCountry(
            id = "us",
            name = "United States",
            code = "US",
            image = "",
            alpha3 = "USA",
            continent = "America",
            defaultCurrency = usdCurrency,
            onrampAvailable = true,
        )
        val confirmResidency = OnrampAvailability.ConfirmResidency(country = usdCountry)

        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = confirmResidency.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNotNull()
        val amounts = result!!.amounts
        assertThat(amounts.map { it.displayValue }).containsExactly(
            stringReference("$50"),
            stringReference("$200"),
            stringReference("$700"),
            resourceReference(R.string.quick_top_up_chip_other),
        ).inOrder()
    }

    @Test
    fun `returns null when onramp availability is error`() {
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = OnrampError.DataError(code = "error", description = null).left(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when balance is loading (amount is null)`() {
        val loadingStatus: CryptoCurrencyStatus = mockk {
            every { value } returns CryptoCurrencyStatus.Loading
        }

        val result = factory.build(
            currencyStatus = loadingStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = availableUsd.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when tx history is not implemented`() {
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = TxHistoryStateError.TxHistoryNotImplemented.left(),
            onrampAvailability = availableUsd.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when tx history fetch fails with data error`() {
        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = TxHistoryStateError.DataError(RuntimeException("network error")).left(),
            onrampAvailability = availableUsd.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when ConfirmResidency with non-USD or EUR default currency`() {
        val gbpCountry = OnrampCountry(
            id = "gb",
            name = "United Kingdom",
            code = "GB",
            image = "",
            alpha3 = "GBR",
            continent = "Europe",
            defaultCurrency = gbpCurrency,
            onrampAvailable = true,
        )

        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = OnrampAvailability.ConfirmResidency(country = gbpCountry).right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when ConfirmResidency but country does not support onramp`() {
        val restrictedCountry = OnrampCountry(
            id = "kp",
            name = "North Korea",
            code = "KP",
            image = "",
            alpha3 = "PRK",
            continent = "Asia",
            defaultCurrency = usdCurrency,
            onrampAvailable = false,
        )
        val confirmResidency = OnrampAvailability.ConfirmResidency(country = restrictedCountry)

        val result = factory.build(
            currencyStatus = zeroBalanceStatus,
            isTxHistoryEmpty = emptyHistory,
            onrampAvailability = confirmResidency.right(),
            onPresetClick = { _, _ -> },
            onOtherClick = {},
        )

        assertThat(result).isNull()
    }
}