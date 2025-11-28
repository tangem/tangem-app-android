package com.tangem.domain.onramp

import com.google.common.truth.Truth
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampAmount
import com.tangem.domain.onramp.model.OnrampOfferAdvantages
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProvider
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.PaymentMethodStatus
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOnrampAllOffersUseCaseTest {

    private val onrampRepository: OnrampRepository = mockk(relaxUnitFun = true)
    private val errorResolver: OnrampErrorResolver = mockk(relaxUnitFun = true)
    private val settingsRepository: SettingsRepository = mockk(relaxUnitFun = true)
    private val cryptoCurrencyId: CryptoCurrency.ID = mockk(relaxUnitFun = true)
    private val userWalletId: UserWalletId = mockk(relaxUnitFun = true)

    private lateinit var useCase: GetOnrampAllOffersUseCase

    @BeforeEach
    fun setup() {
        clearMocks(onrampRepository, errorResolver, settingsRepository, cryptoCurrencyId)
        useCase = GetOnrampAllOffersUseCase(
            onrampRepository = onrampRepository,
            errorResolver = errorResolver,
            settingsRepository = settingsRepository,
        )
    }

    @Test
    fun `should return empty list when no quotes`() = runTest {
        val emptyQuotes = listOf<OnrampQuote>()
        coEvery { onrampRepository.getQuotes() } returns flowOf(emptyQuotes)

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers -> Truth.assertThat(offers).isEmpty() },
            )
        }
        coVerify { onrampRepository.getQuotes() }
    }

    @Test
    fun `should return empty list when only Error quotes`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider = createMockProvider("provider1", "Provider 1")

        val quotes = listOf(
            createMockErrorQuote(paymentMethod, provider),
            createMockErrorQuote(paymentMethod, provider),
        )

        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers -> Truth.assertThat(offers).isEmpty() },
            )
        }
    }

    @Test
    fun `should group offers by payment method with best rate marked`() = runTest {
        val paymentMethod1 = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val paymentMethod2 = createMockPaymentMethod("bank", "Bank Transfer", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")

        val quotes = listOf(
            createMockDataQuote(paymentMethod1, provider1, BigDecimal("100.0")),
            createMockDataQuote(paymentMethod1, provider2, BigDecimal("95.0")),
            createMockDataQuote(paymentMethod2, provider1, BigDecimal("98.0")),
        )

        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(2)

                    val cardGroup = offers.find { it.paymentMethod.id == "card" }
                    Truth.assertThat(cardGroup).isNotNull()
                    Truth.assertThat(cardGroup?.offers).hasSize(2)
                    Truth.assertThat(cardGroup?.providerCount).isEqualTo(2)
                    Truth.assertThat(cardGroup?.isBestPaymentMethod).isTrue()
                    Truth.assertThat(cardGroup?.methodStatus).isEqualTo(PaymentMethodStatus.Available)

                    val bestRateOffer = cardGroup?.offers?.find { it.advantages == OnrampOfferAdvantages.BestRate }
                    Truth.assertThat(bestRateOffer).isNotNull()

                    val bankGroup = offers.find { it.paymentMethod.id == "bank" }
                    Truth.assertThat(bankGroup).isNotNull()
                    Truth.assertThat(bankGroup?.offers).hasSize(1)
                    Truth.assertThat(bankGroup?.providerCount).isEqualTo(1)
                    Truth.assertThat(bankGroup?.isBestPaymentMethod).isFalse()
                    Truth.assertThat(bankGroup?.methodStatus).isEqualTo(PaymentMethodStatus.Available)
                },
            )
        }

        coVerify { onrampRepository.getQuotes() }
        coVerify { settingsRepository.isGooglePayAvailability() }
    }

    @Test
    fun `should sort offers by toAmount descending`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider = createMockProvider("provider1", "Provider 1")

        val quotes = listOf(
            createMockDataQuote(paymentMethod, provider, BigDecimal("90.0")),
            createMockDataQuote(paymentMethod, provider, BigDecimal("100.0")),
            createMockDataQuote(paymentMethod, provider, BigDecimal("95.0")),
        )

        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)
                    val group = offers.first()
                    Truth.assertThat(group.offers).hasSize(3)

                    val amounts = group.offers.map { offer ->
                        when (val quote = offer.quote) {
                            is OnrampQuote.Data -> quote.toAmount.value
                            else -> BigDecimal.ZERO
                        }
                    }
                    Truth.assertThat(amounts).containsExactly(
                        BigDecimal("100.0"),
                        BigDecimal("95.0"),
                        BigDecimal("90.0"),
                    ).inOrder()
                },
            )
        }
    }

    @Test
    fun `should include AmountError offers in group`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")

        val quotes = listOf(
            createMockDataQuote(paymentMethod, provider1, BigDecimal("100.0")),
            createMockAmountErrorQuote(paymentMethod, provider2),
        )

        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)

                    val cardGroup = offers.first()
                    Truth.assertThat(cardGroup.offers).hasSize(2)
                    Truth.assertThat(cardGroup.methodStatus).isEqualTo(PaymentMethodStatus.Available)

                    val dataOffer = cardGroup.offers.find { it.quote is OnrampQuote.Data }
                    Truth.assertThat(dataOffer).isNotNull()
                    Truth.assertThat(dataOffer?.rateDif).isNull() // Best rate offer has null rateDif

                    val amountErrorOffer = cardGroup.offers.find { it.quote is OnrampQuote.AmountError }
                    Truth.assertThat(amountErrorOffer).isNotNull()
                    Truth.assertThat(amountErrorOffer?.rateDif).isNull()
                    Truth.assertThat(amountErrorOffer?.advantages).isEqualTo(OnrampOfferAdvantages.Default)
                },
            )
        }
    }

    @Test
    fun `should mark payment method as Unavailable when only AmountError offers`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")

        val quotes = listOf(
            createMockAmountErrorQuote(paymentMethod, provider1, requiredAmount = BigDecimal("100.0")),
            createMockAmountErrorQuote(paymentMethod, provider2, requiredAmount = BigDecimal("50.0")),
        )

        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)

                    val cardGroup = offers.first()
                    Truth.assertThat(
                        cardGroup.methodStatus,
                    ).isEqualTo(PaymentMethodStatus.Unavailable.MinAmount(BigDecimal("50.0")))
                    Truth.assertThat(cardGroup.offers).hasSize(2)

                    Truth.assertThat(cardGroup.bestRateOffer).isNotNull()
                    val bestQuote = cardGroup.bestRateOffer?.quote as? OnrampQuote.AmountError
                    Truth.assertThat(bestQuote?.error?.requiredAmount).isEqualTo(BigDecimal("50.0"))

                    Truth.assertThat(cardGroup.isBestPaymentMethod).isFalse()

                    cardGroup.offers.forEach { offer ->
                        Truth.assertThat(offer.quote).isInstanceOf(OnrampQuote.AmountError::class.java)
                        Truth.assertThat(offer.rateDif).isNull()
                    }
                },
            )
        }
    }

    @Test
    fun `should use largest required amount for TooBig errors`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")
        val provider3 = createMockProvider("provider3", "Provider 3")

        val quotes = listOf(
            createMockAmountErrorQuote(
                paymentMethod = paymentMethod,
                provider = provider1,
                requiredAmount = BigDecimal("150.0"),
                isMinAmountError = false,
            ),
            createMockAmountErrorQuote(
                paymentMethod = paymentMethod,
                provider = provider2,
                requiredAmount = BigDecimal("75.0"),
                isMinAmountError = false,
            ),
            createMockAmountErrorQuote(
                paymentMethod = paymentMethod,
                provider = provider3,
                requiredAmount = BigDecimal("300.0"),
                isMinAmountError = false,
            ),
        )

        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)

                    val cardGroup = offers.first()
                    Truth.assertThat(cardGroup.offers).hasSize(3)
                    Truth.assertThat(
                        cardGroup.methodStatus,
                    ).isEqualTo(PaymentMethodStatus.Unavailable.MaxAmount(BigDecimal("300.0")))
                },
            )
        }
    }

    @Test
    fun `should not include Error quotes in groups`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")

        val quotes = listOf(
            createMockDataQuote(paymentMethod, provider1, BigDecimal("100.0")),
            createMockErrorQuote(paymentMethod, provider2),
        )

        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)

                    val cardGroup = offers.first()
                    Truth.assertThat(cardGroup.offers).hasSize(1)
                    Truth.assertThat(cardGroup.offers.first().quote).isInstanceOf(OnrampQuote.Data::class.java)
                    Truth.assertThat(cardGroup.methodStatus).isEqualTo(PaymentMethodStatus.Available)
                },
            )
        }
    }

    @Test
    fun `should prioritize Data over AmountError for best rate`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")
        val provider3 = createMockProvider("provider3", "Provider 3")

        val quotes = listOf(
            createMockDataQuote(paymentMethod, provider1, BigDecimal("90.0")),
            createMockAmountErrorQuote(paymentMethod, provider2, requiredAmount = BigDecimal("10.0")),
            createMockDataQuote(paymentMethod, provider3, BigDecimal("100.0")),
        )

        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)

                    val cardGroup = offers.first()
                    Truth.assertThat(cardGroup.offers).hasSize(3)
                    Truth.assertThat(cardGroup.methodStatus).isEqualTo(PaymentMethodStatus.Available)

                    Truth.assertThat(cardGroup.bestRateOffer).isNotNull()
                    val bestQuote = cardGroup.bestRateOffer?.quote as? OnrampQuote.Data
                    Truth.assertThat(bestQuote?.toAmount?.value).isEqualTo(BigDecimal("100.0"))
                },
            )
        }
    }

    @Test
    fun `should sort AmountError offers to bottom`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")
        val provider3 = createMockProvider("provider3", "Provider 3")

        val quotes = listOf(
            createMockAmountErrorQuote(paymentMethod, provider1, requiredAmount = BigDecimal("50.0")),
            createMockDataQuote(paymentMethod, provider2, BigDecimal("95.0")),
            createMockDataQuote(paymentMethod, provider3, BigDecimal("100.0")),
        )

        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)

                    val cardGroup = offers.first()
                    Truth.assertThat(cardGroup.offers).hasSize(3)

                    Truth.assertThat(cardGroup.offers[0].quote).isInstanceOf(OnrampQuote.Data::class.java)
                    Truth.assertThat(cardGroup.offers[1].quote).isInstanceOf(OnrampQuote.Data::class.java)
                    Truth.assertThat(cardGroup.offers[2].quote).isInstanceOf(OnrampQuote.AmountError::class.java)

                    val firstAmount = (cardGroup.offers[0].quote as OnrampQuote.Data).toAmount.value
                    val secondAmount = (cardGroup.offers[1].quote as OnrampQuote.Data).toAmount.value
                    Truth.assertThat(firstAmount).isEqualTo(BigDecimal("100.0"))
                    Truth.assertThat(secondAmount).isEqualTo(BigDecimal("95.0"))
                },
            )
        }
    }

    @Test
    fun `should handle mixed Data AmountError and Error quotes correctly`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")
        val provider3 = createMockProvider("provider3", "Provider 3")
        val provider4 = createMockProvider("provider4", "Provider 4")

        val quotes = listOf(
            createMockDataQuote(paymentMethod, provider1, BigDecimal("100.0")),
            createMockAmountErrorQuote(paymentMethod, provider2, requiredAmount = BigDecimal("50.0")),
            createMockErrorQuote(paymentMethod, provider3),
            createMockDataQuote(paymentMethod, provider4, BigDecimal("95.0")),
        )

        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { settingsRepository.isGooglePayAvailability() } returns false

        val result = useCase(userWalletId, cryptoCurrencyId)

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)

                    val cardGroup = offers.first()
                    Truth.assertThat(cardGroup.offers).hasSize(3)
                    Truth.assertThat(cardGroup.methodStatus).isEqualTo(PaymentMethodStatus.Available)

                    val dataOffers = cardGroup.offers.filter { it.quote is OnrampQuote.Data }
                    val amountErrorOffers = cardGroup.offers.filter { it.quote is OnrampQuote.AmountError }
                    val errorOffers = cardGroup.offers.filter { it.quote is OnrampQuote.Error }

                    Truth.assertThat(dataOffers).hasSize(2)
                    Truth.assertThat(amountErrorOffers).hasSize(1)
                    Truth.assertThat(errorOffers).isEmpty()
                },
            )
        }
    }

    private fun createMockPaymentMethod(id: String, name: String, type: PaymentMethodType): OnrampPaymentMethod {
        return mockk<OnrampPaymentMethod> {
            every { this@mockk.id } returns id
            every { this@mockk.name } returns name
            every { this@mockk.type } returns type
        }
    }

    private fun createMockProvider(id: String, name: String): OnrampProvider {
        return mockk<OnrampProvider> {
            every { this@mockk.id } returns id
            every { this@mockk.info.name } returns name
        }
    }

    private fun createMockDataQuote(
        paymentMethod: OnrampPaymentMethod,
        provider: OnrampProvider,
        toAmount: BigDecimal,
    ): OnrampQuote.Data {
        return mockk<OnrampQuote.Data> {
            every { this@mockk.paymentMethod } returns paymentMethod
            every { this@mockk.provider } returns provider
            every { this@mockk.toAmount } returns createMockAmount(toAmount)
            every { this@mockk.fromAmount } returns createMockAmount(BigDecimal("10.0"))
            every { this@mockk.countryCode } returns "US"
            every { this@mockk.minFromAmount } returns null
            every { this@mockk.maxFromAmount } returns null
        }
    }

    private fun createMockAmountErrorQuote(
        paymentMethod: OnrampPaymentMethod,
        provider: OnrampProvider,
        requiredAmount: BigDecimal = BigDecimal("50.0"),
        isMinAmountError: Boolean = true,
    ): OnrampQuote.AmountError {
        return mockk<OnrampQuote.AmountError> {
            every { this@mockk.paymentMethod } returns paymentMethod
            every { this@mockk.provider } returns provider
            every { this@mockk.fromAmount } returns createMockAmount(BigDecimal("10.0"))
            every { this@mockk.countryCode } returns "US"
            every { this@mockk.error } returns if (isMinAmountError) {
                OnrampError.AmountError.TooSmallError(requiredAmount = requiredAmount)
            } else {
                OnrampError.AmountError.TooBigError(requiredAmount = requiredAmount)
            }
        }
    }

    private fun createMockErrorQuote(paymentMethod: OnrampPaymentMethod, provider: OnrampProvider): OnrampQuote.Error {
        return mockk<OnrampQuote.Error> {
            every { this@mockk.paymentMethod } returns paymentMethod
            every { this@mockk.provider } returns provider
            every { this@mockk.fromAmount } returns createMockAmount(BigDecimal("10.0"))
            every { this@mockk.countryCode } returns "US"
            every { this@mockk.error } returns mockk<OnrampError>()
        }
    }

    private fun createMockAmount(value: BigDecimal): OnrampAmount {
        return mockk<OnrampAmount> {
            every { this@mockk.value } returns value
            every { this@mockk.symbol } returns "USD"
        }
    }
}