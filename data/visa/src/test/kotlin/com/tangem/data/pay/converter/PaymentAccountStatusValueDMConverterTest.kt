package com.tangem.data.pay.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.visa.entity.PaymentAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentAccountStatusValueDMConverterTest {

    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory = mockk()
    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val cryptoCurrency: CryptoCurrency.Token = mockk()

    init {
        every { tangemPayCurrencyFactory.create(userWalletId) } returns cryptoCurrency
    }

    private val converter = PaymentAccountStatusValueDMConverter(tangemPayCurrencyFactory)

    private fun cryptoBalance() = PaymentAccountStatusValue.CryptoBalance(
        id = "usd-coin",
        chainId = 137,
        depositAddress = "0xDEPOSIT",
        tokenContractAddress = "0xCONTRACT",
        balance = BigDecimal("10"),
    )

    private fun cryptoBalanceDM() = PaymentAccountStatusValueDM.CryptoBalanceDM(
        id = "usd-coin",
        chainId = 137,
        depositAddress = "0xDEPOSIT",
        tokenContractAddress = "0xCONTRACT",
        balance = BigDecimal("10"),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @Test
        fun `GIVEN domain Empty WHEN convert THEN returns DM Empty`() {
            // GIVEN
            val domain = PaymentAccountStatusValue.Empty

            // WHEN
            val result = converter.convert(domain)

            // THEN
            assertThat(result).isInstanceOf(PaymentAccountStatusValueDM.Empty::class.java)
        }

        @Test
        fun `GIVEN domain Deactivated with balance WHEN convert THEN returns DM DeactivatedAccount with same balance`() {
            // GIVEN
            val domain = PaymentAccountStatusValue.Deactivated(
                source = StatusSource.ACTUAL,
                fiatBalance = PaymentAccountStatusValue.FiatBalance(
                    availableBalance = BigDecimal("100"),
                    currency = "USD",
                ),
                cryptoBalance = cryptoBalance(),
                cryptoCurrency = cryptoCurrency,
                fiatRate = BigDecimal("1.05"),
            )

            // WHEN
            val result = converter.convert(domain)

            // THEN
            assertThat(result).isInstanceOf(PaymentAccountStatusValueDM.DeactivatedAccount::class.java)
            val dm = result as PaymentAccountStatusValueDM.DeactivatedAccount
            assertThat(dm.fiatBalance.availableBalance).isEqualTo(BigDecimal("100"))
            assertThat(dm.fiatBalance.currency).isEqualTo("USD")
            assertThat(dm.fiatRate).isEqualTo(BigDecimal("1.05"))
        }

        @Test
        fun `GIVEN domain Loading WHEN convert THEN returns null (transient, not persisted)`() {
            // GIVEN
            val domain = PaymentAccountStatusValue.Loading

            // WHEN
            val result = converter.convert(domain)

            // THEN
            assertThat(result).isNull()
        }

        @Test
        fun `GIVEN domain Error Unavailable WHEN convert THEN returns null (transient, not persisted)`() {
            // GIVEN
            val domain = PaymentAccountStatusValue.Error.Unavailable

            // WHEN
            val result = converter.convert(domain)

            // THEN
            assertThat(result).isNull()
        }

        @Test
        fun `GIVEN domain NotCreated WHEN convert THEN returns DM NotCreated`() {
            // GIVEN
            val domain = PaymentAccountStatusValue.NotCreated

            // WHEN
            val result = converter.convert(domain)

            // THEN
            assertThat(result).isInstanceOf(PaymentAccountStatusValueDM.NotCreated::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @Test
        fun `GIVEN DM Empty WHEN convertBack THEN returns domain Empty`() {
            // GIVEN
            val dm = PaymentAccountStatusValueDM.Empty()

            // WHEN
            val result = converter.convertBack(userWalletId, dm)

            // THEN
            assertThat(result).isEqualTo(PaymentAccountStatusValue.Empty)
        }

        @Test
        fun `GIVEN DM DeactivatedAccount WHEN convertBack THEN returns domain Deactivated with CACHE source`() {
            // GIVEN
            val dm = PaymentAccountStatusValueDM.DeactivatedAccount(
                fiatBalance = PaymentAccountStatusValueDM.FiatBalanceDM(
                    availableBalance = BigDecimal("200"),
                    currency = "EUR",
                ),
                cryptoBalance = cryptoBalanceDM(),
                fiatRate = BigDecimal("0.92"),
            )

            // WHEN
            val result = converter.convertBack(userWalletId, dm)

            // THEN
            assertThat(result).isInstanceOf(PaymentAccountStatusValue.Deactivated::class.java)
            val deactivated = result as PaymentAccountStatusValue.Deactivated
            assertThat(deactivated.source).isEqualTo(StatusSource.CACHE)
            assertThat(deactivated.fiatBalance.availableBalance).isEqualTo(BigDecimal("200"))
            assertThat(deactivated.fiatBalance.currency).isEqualTo("EUR")
            assertThat(deactivated.fiatRate).isEqualTo(BigDecimal("0.92"))
        }

        @Test
        fun `GIVEN null DM WHEN convertBack THEN returns Error Unavailable`() {
            // GIVEN / WHEN
            val result = converter.convertBack(userWalletId, null)

            // THEN
            assertThat(result).isEqualTo(PaymentAccountStatusValue.Error.Unavailable)
        }

        @Test
        fun `GIVEN DM NotCreated WHEN convertBack THEN returns domain NotCreated`() {
            // GIVEN
            val dm = PaymentAccountStatusValueDM.NotCreated()

            // WHEN
            val result = converter.convertBack(userWalletId, dm)

            // THEN
            assertThat(result).isEqualTo(PaymentAccountStatusValue.NotCreated)
        }
    }
}