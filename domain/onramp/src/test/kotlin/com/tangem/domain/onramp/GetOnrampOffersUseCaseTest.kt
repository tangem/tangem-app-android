package com.tangem.domain.onramp

import com.google.common.truth.Truth
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOnrampOffersUseCaseTest {

    private val onrampRepository: OnrampRepository = mockk(relaxUnitFun = true)
    private val onrampTransactionRepository: OnrampTransactionRepository = mockk(relaxUnitFun = true)
    private val errorResolver: OnrampErrorResolver = mockk(relaxUnitFun = true)
    private val settingsRepository: SettingsRepository = mockk(relaxUnitFun = true)
    private val cryptoCurrencyId: CryptoCurrency.ID = mockk(relaxUnitFun = true)
    private val promoRepository: PromoRepository = mockk(relaxUnitFun = true)

    private lateinit var useCase: GetOnrampOffersUseCase

    @BeforeEach
    fun setup() {
        clearMocks(onrampRepository, onrampTransactionRepository, errorResolver, cryptoCurrencyId)
        useCase = GetOnrampOffersUseCase(
            onrampRepository = onrampRepository,
            onrampTransactionRepository = onrampTransactionRepository,
            errorResolver = errorResolver,
            settingsRepository = settingsRepository,
            promoRepository = promoRepository,
        )
    }

    @Test
    fun `invoke should return empty list when no valid quotes`() = runTest {
        val emptyQuotes = listOf<OnrampQuote>()
        val emptyTransactions = listOf<OnrampTransaction>()

        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(emptyQuotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(
            emptyTransactions,
        )

        val result = useCase()

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers -> Truth.assertThat(offers).isEmpty() },
            )
        }

        coVerify { onrampRepository.getQuotes() }
        coVerify { onrampTransactionRepository.getAllTransactions() }
    }

    @Test
    fun `invoke should return offers blocks with recent and recommended categories`() = runTest {
        val paymentMethod1 = createMockPaymentMethod("card", "Card", PaymentMethodType.GOOGLE_PAY)
        val paymentMethod2 = createMockPaymentMethod("bank", "Bank Transfer", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")

        val quotes = listOf(
            createMockQuote(paymentMethod1, provider1, BigDecimal("90.0")),
            createMockQuote(paymentMethod2, provider2, BigDecimal("100.0")),
        )

        val transactions = listOf(
            createMockTransaction("Provider 1", "Card", 1000L),
        )

        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(
            transactions,
        )

        val result = useCase()

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(2)

                    val recentBlock = offers.find { it.category == OnrampOfferCategory.Recent }
                    Truth.assertThat(recentBlock).isNotNull()
                    Truth.assertThat(recentBlock?.offers).hasSize(1)
                    Truth.assertThat(recentBlock?.offers?.first()?.advantages).isEqualTo(OnrampOfferAdvantages.Fastest)

                    val recommendedBlock = offers.find { it.category == OnrampOfferCategory.Recommended }
                    Truth.assertThat(recommendedBlock).isNotNull()
                    Truth.assertThat(recommendedBlock?.offers).hasSize(1)
                    Truth.assertThat(recommendedBlock?.offers?.first()?.advantages)
                        .isEqualTo(OnrampOfferAdvantages.GreatRate)
                },
            )
        }
    }

    @Test
    fun `invoke should find best rate offer correctly`() = runTest {
        val paymentMethod1 = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val paymentMethod2 = createMockPaymentMethod("bank", "Bank Transfer", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")

        val quotes = listOf(
            createMockQuote(paymentMethod1, provider1, BigDecimal("90.0")),
            createMockQuote(paymentMethod2, provider2, BigDecimal("100.0")),
            createMockQuote(paymentMethod1, provider1, BigDecimal("95.0")),
        )

        val transactions = emptyList<OnrampTransaction>()

        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(
            transactions,
        )

        val result = useCase()

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)

                    val recommendedBlock = offers.find { it.category == OnrampOfferCategory.Recommended }
                    Truth.assertThat(recommendedBlock).isNotNull()
                    Truth.assertThat(recommendedBlock?.offers).hasSize(1)

                    val grateRateOffer = recommendedBlock?.offers?.first()
                    Truth.assertThat(grateRateOffer?.advantages).isEqualTo(OnrampOfferAdvantages.GreatRate)

                    when (val quote = grateRateOffer?.quote) {
                        is OnrampQuote.Data -> Truth.assertThat(quote.toAmount.value).isEqualTo(BigDecimal("100.0"))
                        else -> Truth.assertThat(false).isTrue()
                    }
                },
            )
        }
    }

    @Test
    fun `invoke should find fastest offer correctly`() = runTest {
        val instantPaymentMethod =
            createMockPaymentMethod("card", "Card", PaymentMethodType.GOOGLE_PAY)
        val slowPaymentMethod =
            createMockPaymentMethod("bank", "Bank Transfer", PaymentMethodType.CARD)
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")

        val quotes = listOf(
            createMockQuote(instantPaymentMethod, provider1, BigDecimal("90.0")),
            createMockQuote(slowPaymentMethod, provider2, BigDecimal("100.0")),
        )

        val transactions = emptyList<OnrampTransaction>()

        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(
            transactions,
        )

        val result = useCase()

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)

                    val recommendedBlock = offers.find { it.category == OnrampOfferCategory.Recommended }
                    Truth.assertThat(recommendedBlock).isNotNull()
                    Truth.assertThat(recommendedBlock?.offers).hasSize(2)

                    val grateRateOffer = recommendedBlock
                        ?.offers
                        ?.find { it.advantages == OnrampOfferAdvantages.GreatRate }
                    val fastestOffer = recommendedBlock
                        ?.offers
                        ?.find { it.advantages == OnrampOfferAdvantages.Fastest }

                    Truth.assertThat(grateRateOffer).isNotNull()
                    Truth.assertThat(fastestOffer).isNotNull()

                    when (val quote = grateRateOffer?.quote) {
                        is OnrampQuote.Data -> Truth.assertThat(quote.toAmount.value).isEqualTo(BigDecimal("100.0"))
                        else -> Truth.assertThat(false).isTrue()
                    }

                    when (val quote = fastestOffer?.quote) {
                        is OnrampQuote.Data -> Truth.assertThat(quote.toAmount.value).isEqualTo(BigDecimal("90.0"))
                        else -> Truth.assertThat(false).isTrue()
                    }
                },
            )
        }
    }

    @Test
    fun `invoke should show recommended block when only one method and provider`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider = createMockProvider("provider1", "Provider 1")

        val quotes = listOf(
            createMockQuote(paymentMethod, provider, BigDecimal("100.0")),
        )

        val transactions = emptyList<OnrampTransaction>()

        coEvery { promoRepository.isMoonpayPromoActive() } returns false
        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(
            transactions,
        )

        val result = useCase()

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).isNotEmpty()
                    Truth.assertThat(offers).hasSize(1)
                },
            )
        }
    }

    @Test
    fun `invoke should fallback to standard instant offers when promo is active but no Moonpay offers exist`() =
        runTest {
            val instantMethod = createMockPaymentMethod("gpay", "Google Pay", PaymentMethodType.GOOGLE_PAY)
            val slowMethod = createMockPaymentMethod("bank", "Bank Transfer", PaymentMethodType.CARD)
            val provider = createMockProvider("other", "Other Provider")

            val quotes = listOf(
                createMockQuote(instantMethod, provider, BigDecimal("95.0")),
                createMockQuote(slowMethod, provider, BigDecimal("100.0")),
            )

            val transactions = emptyList<OnrampTransaction>()

            coEvery { promoRepository.isMoonpayPromoActive() } returns true
            coEvery { settingsRepository.isGooglePayAvailability() } returns true
            coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
            coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(transactions)

            val result = useCase()

            result.collect { either ->
                Truth.assertThat(either.isRight()).isTrue()
                either.fold(
                    ifLeft = { error -> Truth.assertThat(error).isNull() },
                    ifRight = { offers ->
                        Truth.assertThat(offers).hasSize(1)

                        val recommendedBlock = offers.find { it.category == OnrampOfferCategory.Recommended }
                        Truth.assertThat(recommendedBlock).isNotNull()
                        Truth.assertThat(recommendedBlock?.offers).hasSize(2)

                        val fastestOffer =
                            recommendedBlock?.offers?.find { it.advantages == OnrampOfferAdvantages.Fastest }
                        Truth.assertThat(fastestOffer).isNotNull()

                        when (val quote = fastestOffer?.quote) {
                            is OnrampQuote.Data -> {
                                Truth.assertThat(quote.provider.id).isNotEqualTo("moonpay")
                                Truth.assertThat(quote.paymentMethod.type).isEqualTo(PaymentMethodType.GOOGLE_PAY)
                            }
                            else -> Truth.assertThat(false).isTrue()
                        }
                    },
                )
            }
        }

    @Test
    fun `invoke should show Moonpay fastest offer when promo is active`() = runTest {
        val moonpayGooglePayMethod = createMockPaymentMethod("moonpay-gpay", "Google Pay", PaymentMethodType.GOOGLE_PAY)
        val otherGooglePayMethod = createMockPaymentMethod(
            "other-gpay",
            "Other Google Pay",
            PaymentMethodType.GOOGLE_PAY,
        )
        val slowMethod = createMockPaymentMethod("bank", "Bank Transfer", PaymentMethodType.CARD)
        val moonpayProvider = createMockProvider("moonpay", "Moonpay")
        val otherProvider = createMockProvider("other", "Other Provider")

        val quotes = listOf(
            createMockQuote(moonpayGooglePayMethod, moonpayProvider, BigDecimal("100.0")),
            createMockQuote(otherGooglePayMethod, otherProvider, BigDecimal("95.0")),
            createMockQuote(slowMethod, otherProvider, BigDecimal("105.0")),
        )

        val transactions = emptyList<OnrampTransaction>()

        coEvery { promoRepository.isMoonpayPromoActive() } returns true
        coEvery { settingsRepository.isGooglePayAvailability() } returns true
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(transactions)

        val result = useCase()

        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)

                    val recommendedBlock = offers.find { it.category == OnrampOfferCategory.Recommended }
                    Truth.assertThat(recommendedBlock).isNotNull()

                    val fastestOffer = recommendedBlock?.offers?.find { it.advantages == OnrampOfferAdvantages.Fastest }
                    Truth.assertThat(fastestOffer).isNotNull()

                    when (val quote = fastestOffer?.quote) {
                        is OnrampQuote.Data -> {
                            Truth.assertThat(quote.provider.id).isEqualTo("moonpay")
                            Truth.assertThat(quote.paymentMethod.type).isEqualTo(PaymentMethodType.GOOGLE_PAY)
                            Truth.assertThat(quote.toAmount.value).isEqualTo(BigDecimal("100.0"))
                        }
                        else -> Truth.assertThat(false).isTrue()
                    }
                },
            )
        }
    }

    private fun createMockPaymentMethod(
        id: String,
        name: String,
        type: PaymentMethodType = PaymentMethodType.CARD,
    ): OnrampPaymentMethod {
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

    private fun createMockQuote(
        paymentMethod: OnrampPaymentMethod,
        provider: OnrampProvider,
        toAmount: BigDecimal,
    ): OnrampQuote.Data {
        return mockk<OnrampQuote.Data> {
            every { this@mockk.paymentMethod } returns paymentMethod
            every { this@mockk.provider } returns provider
            every { this@mockk.toAmount } returns mockk {
                every { value } returns toAmount
            }
        }
    }

    private fun createMockTransaction(providerName: String, paymentMethod: String, timestamp: Long): OnrampTransaction {
        return mockk<OnrampTransaction> {
            every { this@mockk.providerName } returns providerName
            every { this@mockk.paymentMethod } returns paymentMethod
            every { this@mockk.timestamp } returns timestamp
            every { this@mockk.status } returns OnrampStatus.Status.Finished
        }
    }
}