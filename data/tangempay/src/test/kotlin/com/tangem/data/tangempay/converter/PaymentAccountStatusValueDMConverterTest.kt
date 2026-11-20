package com.tangem.data.tangempay.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.tangempay.entity.PaymentAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.tangempay.TangemPayCard
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.domain.models.tangempay.TangemPayCardLimitData
import com.tangem.domain.models.tangempay.TangemPayCardState
import com.tangem.domain.models.tangempay.TangemPayCardType
import com.tangem.domain.models.tangempay.TangemPayCustomerResidency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayCurrencyFactory
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN persisted domain status WHEN convert THEN returns its DM`(model: ConvertModel) {
            // Act
            val actual = converter.convert(model.domain)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @ParameterizedTest
        @MethodSource("provideTransientStatuses")
        fun `GIVEN transient domain status WHEN convert THEN returns null (not persisted)`(
            domain: PaymentAccountStatusValue,
        ) {
            // Act
            val actual = converter.convert(domain)

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = listOf(
            ConvertModel(
                domain = PaymentAccountStatusValue.Empty,
                expected = PaymentAccountStatusValueDM.Empty,
            ),
            ConvertModel(
                domain = PaymentAccountStatusValue.NotCreated,
                expected = PaymentAccountStatusValueDM.NotCreated,
            ),
            ConvertModel(
                domain = PaymentAccountStatusValue.Deactivated(
                    source = StatusSource.ACTUAL,
                    customerId = "customer-1",
                    balance = PaymentAccountStatusValue.Balance(
                        fiatBalance = PaymentAccountStatusValue.FiatBalance(
                            availableBalance = BigDecimal("100"),
                            currency = "USD",
                        ),
                    ),
                    cryptoCurrency = cryptoCurrency,
                    networks = emptyList(),
                    fiatRate = BigDecimal("1.05"),
                    error = null,
                ),
                expected = PaymentAccountStatusValueDM.DeactivatedAccount(
                    customerId = "customer-1",
                    fiatRate = BigDecimal("1.05"),
                    fiatBalance = PaymentAccountStatusValueDM.FiatBalanceDM(
                        availableBalance = BigDecimal("100"),
                        currency = "USD",
                    ),
                ),
            ),
            ConvertModel(
                domain = PaymentAccountStatusValue.Loaded(
                    source = StatusSource.ACTUAL,
                    customerId = "customer-3",
                    paymentAccountAddress = null,
                    balance = null,
                    cryptoCurrency = cryptoCurrency,
                    networks = emptyList(),
                    cards = emptyList(),
                    fiatRate = null,
                    error = null,
                    virtualAccount = null,
                    isBankOfframpAvailable = null,
                    tariffPlan = null,
                    isPaymentAccountIssuing = false,
                ),
                expected = PaymentAccountStatusValueDM.ActiveAccount(
                    customerId = "customer-3",
                    currencyCode = null,
                    paymentAccountAddress = null,
                    fiatBalance = null,
                    fiatRate = null,
                    cards = emptyList(),
                ),
            ),
        )

        private fun provideTransientStatuses() = listOf(
            PaymentAccountStatusValue.Loading,
            PaymentAccountStatusValue.AwaitingPlanSelection(
                source = StatusSource.ACTUAL,
                tariffPlan = mockk(),
            ),
            PaymentAccountStatusValue.Inactive(
                source = StatusSource.ACTUAL,
                fiatBalance = PaymentAccountStatusValue.FiatBalance(
                    availableBalance = BigDecimal("100"),
                    currency = "USD",
                ),
                tariffPlan = mockk(),
            ),
            PaymentAccountStatusValue.Error.Unavailable,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stored DM WHEN convertBack THEN returns its domain status`(model: ConvertBackModel) {
            // Act
            val actual = converter.convertBack(userWalletId, model.dm)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @ParameterizedTest
        @MethodSource("provideRoundTripModels")
        fun `GIVEN card issue failed or KYC required WHEN round tripped THEN restored from cache with payload`(
            model: RoundTripModel,
        ) {
            // Act
            val restored = converter.convertBack(userWalletId, converter.convert(model.domain))

            // Assert
            assertThat(restored).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ConvertBackModel(
                dm = PaymentAccountStatusValueDM.Empty,
                expected = PaymentAccountStatusValue.Empty,
            ),
            ConvertBackModel(
                dm = PaymentAccountStatusValueDM.NotCreated,
                expected = PaymentAccountStatusValue.NotCreated,
            ),
            ConvertBackModel(
                dm = null,
                expected = PaymentAccountStatusValue.Error.Unavailable,
            ),
            ConvertBackModel(
                dm = PaymentAccountStatusValueDM.DeactivatedAccount(
                    customerId = "customer-2",
                    fiatBalance = PaymentAccountStatusValueDM.FiatBalanceDM(
                        availableBalance = BigDecimal("200"),
                        currency = "EUR",
                    ),
                    fiatRate = BigDecimal("0.92"),
                ),
                expected = PaymentAccountStatusValue.Deactivated(
                    source = StatusSource.CACHE,
                    customerId = "customer-2",
                    balance = PaymentAccountStatusValue.Balance(
                        fiatBalance = PaymentAccountStatusValue.FiatBalance(
                            availableBalance = BigDecimal("200"),
                            currency = "EUR",
                        ),
                    ),
                    cryptoCurrency = cryptoCurrency,
                    networks = emptyList(),
                    fiatRate = BigDecimal("0.92"),
                    error = null,
                ),
            ),
            ConvertBackModel(
                dm = PaymentAccountStatusValueDM.ActiveAccount(
                    customerId = "customer-4",
                    currencyCode = null,
                    paymentAccountAddress = null,
                    fiatBalance = null,
                    fiatRate = null,
                    cards = emptyList(),
                ),
                expected = PaymentAccountStatusValue.Loaded(
                    source = StatusSource.CACHE,
                    customerId = "customer-4",
                    paymentAccountAddress = null,
                    balance = null,
                    cryptoCurrency = cryptoCurrency,
                    networks = emptyList(),
                    cards = emptyList(),
                    fiatRate = null,
                    error = null,
                    virtualAccount = null,
                    isBankOfframpAvailable = null,
                    tariffPlan = null,
                    isPaymentAccountIssuing = false,
                ),
            ),
        )

        private fun provideRoundTripModels() = listOf(
            RoundTripModel(
                domain = PaymentAccountStatusValue.Error.CardIssueFailed(
                    customerId = "customer-id",
                    tariffPlan = null,
                    balance = accountBalance(),
                    fiatRate = BigDecimal("1.5"),
                ),
                expected = PaymentAccountStatusValue.Error.CardIssueFailed(
                    customerId = "customer-id",
                    source = StatusSource.CACHE,
                    balance = accountBalance(),
                    fiatRate = BigDecimal("1.5"),
                ),
            ),
            RoundTripModel(
                domain = PaymentAccountStatusValue.Error.CardIssueFailed(customerId = "customer-id"),
                expected = PaymentAccountStatusValue.Error.CardIssueFailed(
                    customerId = "customer-id",
                    source = StatusSource.CACHE,
                ),
            ),
            RoundTripModel(
                domain = PaymentAccountStatusValue.KycRequired(
                    source = StatusSource.ACTUAL,
                    customerId = "customer-id",
                    customerResidency = TangemPayCustomerResidency(countryCode = "US", regionCode = "TX"),
                ),
                expected = PaymentAccountStatusValue.KycRequired(
                    source = StatusSource.CACHE,
                    customerId = "customer-id",
                    customerResidency = null,
                ),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CardStateRoundTrip {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN card state, frozen state and type WHEN round tripped THEN the card is restored`(
            model: CardStateModel,
        ) {
            // Arrange
            val card = card(state = model.state, frozenState = model.frozenState, cardType = model.cardType)
            val domain = loadedWithCard(card)

            // Act
            val restored = converter.convertBack(userWalletId, converter.convert(domain))

            // Assert
            val restoredCard = (restored as PaymentAccountStatusValue.Loaded).cards.single()
            val expected = card.copy(limit = TangemPayCardLimitData(actualCardLimit = null, adminCardLimit = null))
            assertThat(restoredCard).isEqualTo(expected)
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

    internal data class ConvertModel(
        val domain: PaymentAccountStatusValue,
        val expected: PaymentAccountStatusValueDM,
    )

    internal data class ConvertBackModel(
        val dm: PaymentAccountStatusValueDM?,
        val expected: PaymentAccountStatusValue,
    )

    internal data class RoundTripModel(
        val domain: PaymentAccountStatusValue,
        val expected: PaymentAccountStatusValue,
    )

    internal data class CardStateModel(
        val state: TangemPayCardState,
        val frozenState: TangemPayCardFrozenState,
        val cardType: TangemPayCardType = TangemPayCardType.VIRTUAL,
    ) {
        override fun toString(): String = "$state / $frozenState / $cardType"
    }

    private fun card(
        state: TangemPayCardState,
        frozenState: TangemPayCardFrozenState,
        cardType: TangemPayCardType,
    ) = TangemPayCard(
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
    )

    private fun loadedWithCard(card: TangemPayCard) = PaymentAccountStatusValue.Loaded(
        source = StatusSource.ACTUAL,
        customerId = "cust_1",
        paymentAccountAddress = "0xDEPOSIT",
        balance = PaymentAccountStatusValue.Balance(
            fiatBalance = PaymentAccountStatusValue.FiatBalance(
                availableBalance = BigDecimal("10"),
                currency = "USD",
            ),
        ),
        cryptoCurrency = cryptoCurrency,
        cards = listOf(card),
        fiatRate = null,
        error = null,
        virtualAccount = null,
        isBankOfframpAvailable = null,
        tariffPlan = null,
        isPaymentAccountIssuing = false,
        networks = emptyList(),
    )
}