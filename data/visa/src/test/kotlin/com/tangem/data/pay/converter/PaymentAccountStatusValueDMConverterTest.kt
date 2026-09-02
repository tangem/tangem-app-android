package com.tangem.data.pay.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.visa.entity.PaymentAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.TangemPayCardType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
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

    private fun accountBalance() = PaymentAccountStatusValue.Balance(
        fiatBalance = PaymentAccountStatusValue.FiatBalance(
            availableBalance = BigDecimal("12.34"),
            currency = "USD",
        ),
        cryptoBalance = cryptoBalance(),
        availableForWithdrawal = BigDecimal("10.00"),
    )

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
                customerId = "customer-1",
                balance = PaymentAccountStatusValue.Balance(
                    fiatBalance = PaymentAccountStatusValue.FiatBalance(
                        availableBalance = BigDecimal("100"),
                        currency = "USD",
                    ),
                    cryptoBalance = cryptoBalance(),
                    availableForWithdrawal = BigDecimal("7"),
                ),
                cryptoCurrency = cryptoCurrency,
                networks = emptyList(),
                fiatRate = BigDecimal("1.05"),
                error = null,
            )

            // WHEN
            val result = converter.convert(domain)

            // THEN
            assertThat(result).isInstanceOf(PaymentAccountStatusValueDM.DeactivatedAccount::class.java)
            val dm = result as PaymentAccountStatusValueDM.DeactivatedAccount
            assertThat(dm.customerId).isEqualTo("customer-1")
            assertThat(dm.fiatBalance?.availableBalance).isEqualTo(BigDecimal("100"))
            assertThat(dm.fiatBalance?.currency).isEqualTo("USD")
            assertThat(dm.availableForWithdrawal).isEqualTo(BigDecimal("7"))
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
        fun `GIVEN domain AwaitingPlanSelection WHEN convert THEN returns null (transient, not persisted)`() {
            // GIVEN
            val domain = PaymentAccountStatusValue.AwaitingPlanSelection(
                source = StatusSource.ACTUAL,
                tariffPlan = mockk(),
            )

            // WHEN
            val result = converter.convert(domain)

            // THEN
            assertThat(result).isNull()
        }

        @Test
        fun `GIVEN domain Inactive WHEN convert THEN returns null (transient, not persisted)`() {
            // GIVEN
            val domain = PaymentAccountStatusValue.Inactive(
                source = StatusSource.ACTUAL,
                fiatBalance = PaymentAccountStatusValue.FiatBalance(
                    availableBalance = BigDecimal("100"),
                    currency = "USD",
                ),
                tariffPlan = mockk(),
            )

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
        fun `GIVEN domain Loaded without balance WHEN convert THEN returns DM ActiveAccount without balance`() {
            // GIVEN
            val domain = PaymentAccountStatusValue.Loaded(
                source = StatusSource.ACTUAL,
                customerId = "customer-3",
                depositAddress = null,
                balance = null,
                cryptoCurrency = cryptoCurrency,
                networks = emptyList(),
                cards = emptyList(),
                fiatRate = null,
                error = null,
                virtualAccount = null,
                tariffPlan = null,
            )

            // WHEN
            val result = converter.convert(domain)

            // THEN
            val dm = result as PaymentAccountStatusValueDM.ActiveAccount
            assertThat(dm.customerId).isEqualTo("customer-3")
            assertThat(dm.fiatBalance).isNull()
            assertThat(dm.cryptoBalance).isNull()
            assertThat(dm.currencyCode).isNull()
            assertThat(dm.availableForWithdrawal).isNull()
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
        fun `GIVEN card issue failed with a balance WHEN round tripped THEN the account payload survives`() {
            // Arrange
            val domain = PaymentAccountStatusValue.Error.CardIssueFailed(
                customerId = "customer-id",
                tariffPlan = null,
                balance = accountBalance(),
                fiatRate = BigDecimal("1.5"),
            )

            // Act
            val restored = converter.convertBack(userWalletId, converter.convert(domain))

            // Assert
            assertThat(restored).isEqualTo(
                PaymentAccountStatusValue.Error.CardIssueFailed(
                    customerId = "customer-id",
                    source = StatusSource.CACHE,
                    balance = accountBalance(),
                    fiatRate = BigDecimal("1.5"),
                ),
            )
        }

        @Test
        fun `GIVEN card issue failed without a balance WHEN round tripped THEN it restores from cache`() {
            // Arrange
            val domain = PaymentAccountStatusValue.Error.CardIssueFailed(customerId = "customer-id")

            // Act
            val restored = converter.convertBack(userWalletId, converter.convert(domain))

            // Assert
            assertThat(restored).isEqualTo(
                PaymentAccountStatusValue.Error.CardIssueFailed(
                    customerId = "customer-id",
                    source = StatusSource.CACHE,
                ),
            )
        }

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
                customerId = "customer-2",
                fiatBalance = PaymentAccountStatusValueDM.FiatBalanceDM(
                    availableBalance = BigDecimal("200"),
                    currency = "EUR",
                ),
                cryptoBalance = cryptoBalanceDM(),
                fiatRate = BigDecimal("0.92"),
                availableForWithdrawal = BigDecimal("5"),
            )

            // WHEN
            val result = converter.convertBack(userWalletId, dm)

            // THEN
            assertThat(result).isInstanceOf(PaymentAccountStatusValue.Deactivated::class.java)
            val deactivated = result as PaymentAccountStatusValue.Deactivated
            assertThat(deactivated.source).isEqualTo(StatusSource.CACHE)
            assertThat(deactivated.customerId).isEqualTo("customer-2")
            assertThat(deactivated.balance?.fiatBalance?.availableBalance).isEqualTo(BigDecimal("200"))
            assertThat(deactivated.balance?.fiatBalance?.currency).isEqualTo("EUR")
            assertThat(deactivated.balance?.availableForWithdrawal).isEqualTo(BigDecimal("5"))
            assertThat(deactivated.fiatRate).isEqualTo(BigDecimal("0.92"))
        }

        @Test
        fun `GIVEN DM ActiveAccount without balance WHEN convertBack THEN returns Loaded with null balance`() {
            // GIVEN
            val dm = PaymentAccountStatusValueDM.ActiveAccount(
                customerId = "customer-4",
                currencyCode = null,
                depositAddress = null,
                fiatBalance = null,
                cryptoBalance = null,
                fiatRate = null,
                availableForWithdrawal = null,
                cards = emptyList(),
            )

            // WHEN
            val result = converter.convertBack(userWalletId, dm)

            // THEN
            val loaded = result as PaymentAccountStatusValue.Loaded
            assertThat(loaded.balance).isNull()
            assertThat(loaded.cryptoCurrencyStatus).isNull()
            assertThat(loaded.source).isEqualTo(StatusSource.CACHE)
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CardStateRoundTrip {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: CardStateModel) {
            // GIVEN
            val domain = loadedWithCard(
                state = model.state,
                frozenState = model.frozenState,
                cardType = model.cardType,
            )

            // WHEN
            val restored = converter.convertBack(userWalletId, converter.convert(domain))

            // THEN
            val card = (restored as PaymentAccountStatusValue.Loaded).cards.single()
            assertThat(card.state).isEqualTo(model.state)
            assertThat(card.frozenState).isEqualTo(model.frozenState)
            assertThat(card.cardType).isEqualTo(model.cardType)
            assertThat(card.embossName).isEqualTo("JOHNNY SILVERHAND")
            assertThat(card.lastDigits).isEqualTo("8890")
        }

        private fun provideTestModels() = TangemPayCardState.entries.map { state ->
            CardStateModel(state = state, frozenState = TangemPayCardFrozenState.Unfrozen)
        } + TangemPayCardType.entries.map { cardType ->
            CardStateModel(
                state = TangemPayCardState.Active,
                frozenState = TangemPayCardFrozenState.Unfrozen,
                cardType = cardType,
            )
        } + CardStateModel(
            state = TangemPayCardState.Delivering,
            frozenState = TangemPayCardFrozenState.Frozen,
        )
    }

    internal data class CardStateModel(
        val state: TangemPayCardState,
        val frozenState: TangemPayCardFrozenState,
        val cardType: TangemPayCardType = TangemPayCardType.VIRTUAL,
    ) {
        override fun toString(): String = "$state / $frozenState / $cardType"
    }

    private fun loadedWithCard(
        state: TangemPayCardState,
        frozenState: TangemPayCardFrozenState,
        cardType: TangemPayCardType = TangemPayCardType.VIRTUAL,
    ) = PaymentAccountStatusValue.Loaded(
        source = StatusSource.ACTUAL,
        customerId = "cust_1",
        depositAddress = "0xDEPOSIT",
        balance = PaymentAccountStatusValue.Balance(
            fiatBalance = PaymentAccountStatusValue.FiatBalance(
                availableBalance = BigDecimal("10"),
                currency = "USD",
            ),
            cryptoBalance = cryptoBalance(),
            availableForWithdrawal = BigDecimal("10"),
        ),
        cryptoCurrency = cryptoCurrency,
        cards = listOf(
            TangemPayCard(
                id = "card_1",
                productInstanceId = "pi_1",
                cardStatus = TangemPayCard.Status.INACTIVE,
                hasPinCode = false,
                displayName = null,
                limit = null,
                frozenState = frozenState,
                lastDigits = "8890",
                images = emptyList(),
                state = state,
                embossName = "JOHNNY SILVERHAND",
                cardType = cardType,
            ),
        ),
        fiatRate = null,
        error = null,
        virtualAccount = null,
        tariffPlan = null,
        networks = emptyList(),
    )
}