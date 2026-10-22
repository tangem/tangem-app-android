package com.tangem.features.send.mcp

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.assertEither
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@ResourceLock(Resources.LOCALE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendIntentSendRouteConverterTest {

    private val converter = SendIntentSendRouteConverter()

    private val walletId = UserWalletId("011")
    private val usdt = createToken()
    private val eth = createCoin()

    private lateinit var originalLocale: Locale

    @BeforeEach
    fun saveLocale() {
        originalLocale = Locale.getDefault()
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `GIVEN intent with memo WHEN convert THEN send is prefilled and bound to the intent without a transaction`() {
        // Arrange
        Locale.setDefault(Locale.US)
        val intent = createSendIntent(
            id = "intent-1",
            toAddress = "0xrecipient",
            amount = BigDecimal("12.5"),
            memo = "memo-7",
        )

        // Act
        val actual = converter.convert(
            SendIntentSendRouteConverter.Input(intent = intent, userWalletId = walletId, currency = usdt),
        )

        // Assert
        val expected = AppRoute.Send(
            userWalletId = walletId,
            currency = usdt,
            transactionId = null,
            amount = "12.5",
            tag = "memo-7",
            destinationAddress = "0xrecipient",
            entryType = AppRoute.Send.EntryType.Manual,
            intentId = "intent-1",
        )
        assertEither(actual = actual, expected = expected.right())
    }

    @Test
    fun `GIVEN intent without memo WHEN convert THEN send gets no tag`() {
        // Arrange
        Locale.setDefault(Locale.US)
        val intent = createSendIntent(id = "intent-2", memo = null)

        // Act
        val actual = converter.convert(
            SendIntentSendRouteConverter.Input(intent = intent, userWalletId = walletId, currency = eth),
        )

        // Assert
        val expected = AppRoute.Send(
            userWalletId = walletId,
            currency = eth,
            amount = "42",
            tag = null,
            destinationAddress = intent.toAddress,
            intentId = "intent-2",
        )
        assertEither(actual = actual, expected = expected.right())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN intent amount and locale WHEN convert THEN only exactly representable positive amounts are accepted`(
        model: AmountModel,
    ) {
        // Arrange
        Locale.setDefault(model.locale)
        val intent = createSendIntent(id = "intent-3", amount = BigDecimal(model.amount))

        // Act
        val actual = converter.convert(
            SendIntentSendRouteConverter.Input(intent = intent, userWalletId = walletId, currency = model.currency),
        )

        // Assert
        val expected = model.expected?.let { amount ->
            AppRoute.Send(
                userWalletId = walletId,
                currency = model.currency,
                amount = amount,
                destinationAddress = intent.toAddress,
                intentId = "intent-3",
            ).right()
        } ?: IllegalArgumentException("Intent intent-3 amount cannot fit ${model.currency.decimals} decimals").left()
        assertEither(actual = actual, expected = expected)
        actual.onRight { route ->
            val parsedAmount = checkNotNull(route.amount?.parseBigDecimalOrNull())
            assertThat(parsedAmount.compareTo(intent.amount)).isEqualTo(0)
        }
    }

    @Test
    fun `GIVEN overflowing amount scale WHEN convert THEN returns an error without throwing`() {
        // Arrange
        val intent = createSendIntent(amount = BigDecimal("100E+2147483647"))

        // Act
        val actual = converter.convert(
            SendIntentSendRouteConverter.Input(intent = intent, userWalletId = walletId, currency = usdt),
        )

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(ArithmeticException::class.java)
    }

    data class AmountModel(
        val locale: Locale,
        val currency: CryptoCurrency,
        val amount: String,
        val expected: String?,
    ) {
        override fun toString() = "$locale ${currency.symbol} $amount -> $expected"
    }

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        AmountModel(locale = Locale.US, currency = usdt, amount = "42", expected = "42"),
        AmountModel(locale = Locale.GERMANY, currency = usdt, amount = "42", expected = "42"),
        AmountModel(locale = Locale.US, currency = usdt, amount = "1.5", expected = "1.5"),
        AmountModel(locale = Locale.GERMANY, currency = usdt, amount = "1.5", expected = "1,5"),
        AmountModel(locale = Locale.US, currency = usdt, amount = "1234567.25", expected = "1234567.25"),
        AmountModel(locale = Locale.GERMANY, currency = usdt, amount = "1234567.25", expected = "1234567,25"),
        AmountModel(locale = Locale.US, currency = usdt, amount = "0.123456", expected = "0.123456"),
        AmountModel(locale = Locale.GERMANY, currency = usdt, amount = "0.123456", expected = "0,123456"),
        AmountModel(locale = Locale.US, currency = usdt, amount = "0.000001", expected = "0.000001"),
        AmountModel(locale = Locale.GERMANY, currency = usdt, amount = "0.000001", expected = "0,000001"),
        AmountModel(locale = Locale.US, currency = usdt, amount = "1.2300000", expected = "1.23"),
        AmountModel(locale = Locale.GERMANY, currency = usdt, amount = "1.2300000", expected = "1,23"),
        AmountModel(locale = Locale.US, currency = usdt, amount = "0.1234565", expected = null),
        AmountModel(locale = Locale.GERMANY, currency = usdt, amount = "0.1234564", expected = null),
        AmountModel(locale = Locale.US, currency = usdt, amount = "0.0000004", expected = null),
        AmountModel(locale = Locale.GERMANY, currency = usdt, amount = "0.0000005", expected = null),
        AmountModel(locale = Locale.US, currency = usdt, amount = "1.0000001", expected = null),
        AmountModel(locale = Locale.US, currency = usdt, amount = "0", expected = null),
        AmountModel(locale = Locale.US, currency = usdt, amount = "-1", expected = null),
        AmountModel(locale = Locale.US, currency = eth, amount = "1E-18", expected = "0.000000000000000001"),
        AmountModel(locale = Locale.GERMANY, currency = eth, amount = "1E-18", expected = "0,000000000000000001"),
        AmountModel(locale = Locale.US, currency = eth, amount = "1E-19", expected = null),
        AmountModel(locale = Locale.US, currency = usdt.copy(decimals = 0), amount = "42.000", expected = "42"),
        AmountModel(locale = Locale.US, currency = usdt.copy(decimals = 0), amount = "0.1", expected = null),
        AmountModel(
            locale = Locale.US,
            currency = eth,
            amount = "0.123456789012345678",
            expected = "0.123456789012345678",
        ),
        AmountModel(locale = Locale.US, currency = usdt, amount = "1E+2", expected = "100"),
    )
}