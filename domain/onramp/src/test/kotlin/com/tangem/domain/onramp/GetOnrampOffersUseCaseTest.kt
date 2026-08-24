package com.tangem.domain.onramp

import com.google.common.truth.Truth
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
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

    private lateinit var useCase: GetOnrampOffersUseCase

    @BeforeEach
    fun setup() {
        clearMocks(onrampRepository, onrampTransactionRepository, errorResolver, settingsRepository, cryptoCurrencyId)
        useCase = GetOnrampOffersUseCase(
            onrampRepository = onrampRepository,
            onrampTransactionRepository = onrampTransactionRepository,
            errorResolver = errorResolver,
            settingsRepository = settingsRepository,
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
    fun `GIVEN restricted and valid quotes WHEN invoke THEN restricted is excluded from recommended`() =
        runTest {
            // Arrange
            val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
            val restrictedProvider = createMockProvider("restricted", "Restricted Provider")
            val validProvider = createMockProvider("valid", "Valid Provider")
            val quotes = listOf(
                createRestrictedQuote(paymentMethod, restrictedProvider),
                createMockQuote(paymentMethod, validProvider, BigDecimal("100.0")),
            )
            coEvery { settingsRepository.isGooglePayAvailability() } returns false
            coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
            coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(emptyList())

            // Act
            val result = useCase()

            // Assert
            result.collect { either ->
                Truth.assertThat(either.isRight()).isTrue()
                either.fold(
                    ifLeft = { error -> Truth.assertThat(error).isNull() },
                    ifRight = { offers ->
                        Truth.assertThat(offers).hasSize(1)
                        // The restricted quote has the better rate (120 > 100) but can't be bought, so it
                        // doesn't compete: the valid offer is recommended, and with only one purchasable
                        // candidate it carries no comparative advantage.
                        val recommendedBlock = offers.single()
                        Truth.assertThat(recommendedBlock.category).isEqualTo(OnrampOfferCategory.Recommended)
                        Truth.assertThat(recommendedBlock.hasMoreOffers).isTrue()
                        val offer = recommendedBlock.offers.single()
                        Truth.assertThat(offer.quote.provider.id).isEqualTo("valid")
                        Truth.assertThat(offer.advantages).isEqualTo(OnrampOfferAdvantages.Default)
                        Truth.assertThat(offer.rateDif).isNull()
                    },
                )
            }
        }

    @Test
    fun `GIVEN only restricted quotes WHEN invoke THEN restricted offer shown in recommended block`() = runTest {
        // Arrange
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val provider = createMockProvider("provider1", "Provider 1")
        val quotes = listOf(createRestrictedQuote(paymentMethod, provider))
        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(emptyList())

        // Act
        val result = useCase()

        // Assert
        result.collect { either ->
            Truth.assertThat(either.isRight()).isTrue()
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).hasSize(1)
                    val block = offers.single()
                    Truth.assertThat(block.category).isEqualTo(OnrampOfferCategory.Recommended)
                    Truth.assertThat(block.hasMoreOffers).isFalse()
                    val offer = block.offers.single()
                    Truth.assertThat(offer.quote.provider.id).isEqualTo("provider1")
                    Truth.assertThat(offer.advantages).isEqualTo(OnrampOfferAdvantages.Default)
                    Truth.assertThat(offer.rateDif).isNull()
                },
            )
        }
    }

    @Test
    fun `GIVEN restricted extra quote WHEN invoke THEN restricted quote counts toward hasMoreOffers`() = runTest {
        // Arrange
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val restrictedProvider = createMockProvider("restricted", "Restricted Provider")
        val validProvider = createMockProvider("valid", "Valid Provider")
        val quotes = listOf(
            createMockQuote(paymentMethod, validProvider, BigDecimal("100.0")),
            createRestrictedQuote(paymentMethod, restrictedProvider),
        )
        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(emptyList())

        // Act
        val result = useCase()

        // Assert
        result.collect { either ->
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    Truth.assertThat(offers).isNotEmpty()
                    // The restricted offer is reachable via "All offers", so hasMoreOffers is set
                    Truth.assertThat(offers.all { !it.hasMoreOffers }).isFalse()
                },
            )
        }
    }

    @Test
    fun `GIVEN restricted quote matching recent transaction WHEN invoke THEN restricted is not recent`() =
        runTest {
            // Arrange
            val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
            val restrictedProvider = createMockProvider("restricted", "Restricted Provider")
            val validProvider = createMockProvider("valid", "Valid Provider")
            val quotes = listOf(
                createMockQuote(paymentMethod, validProvider, BigDecimal("100.0")),
                createRestrictedQuote(paymentMethod, restrictedProvider),
            )
            val transactions = listOf(
                createMockTransaction("Restricted Provider", "Card", 1000L),
            )
            coEvery { settingsRepository.isGooglePayAvailability() } returns false
            coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
            coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(transactions)

            // Act
            val result = useCase()

            // Assert
            result.collect { either ->
                either.fold(
                    ifLeft = { error -> Truth.assertThat(error).isNull() },
                    ifRight = { offers ->
                        // The only recently used provider is restricted, so there is no Recent block at all —
                        // the valid quote is recommended instead.
                        Truth.assertThat(offers).hasSize(1)
                        Truth.assertThat(offers.find { it.category == OnrampOfferCategory.Recent }).isNull()

                        val recommendedBlock = offers.single()
                        Truth.assertThat(recommendedBlock.category).isEqualTo(OnrampOfferCategory.Recommended)
                        Truth.assertThat(recommendedBlock.hasMoreOffers).isTrue()
                        val offer = recommendedBlock.offers.single()
                        Truth.assertThat(offer.quote.provider.id).isEqualTo("valid")
                        Truth.assertThat(offer.advantages).isEqualTo(OnrampOfferAdvantages.Default)
                        Truth.assertThat(offer.rateDif).isNull()
                    },
                )
            }
        }

    @Test
    fun `GIVEN three restricted-only quotes WHEN invoke THEN only best recommended and hasMoreOffers true`() = runTest {
        // Arrange
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val quotes = listOf(
            createRestrictedQuote(paymentMethod, createMockProvider("provider0", "Provider 0"), BigDecimal("100.0")),
            createRestrictedQuote(paymentMethod, createMockProvider("provider1", "Provider 1"), BigDecimal("120.0")),
            createRestrictedQuote(paymentMethod, createMockProvider("provider2", "Provider 2"), BigDecimal("90.0")),
        )
        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(emptyList())

        // Act
        val result = useCase()

        // Assert
        result.collect { either ->
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    // Nothing is purchasable, so a single restricted offer (the best rate among them,
                    // provider1 at 120) stands in with no advantage; the other two stay in "All offers".
                    val block = offers.single()
                    Truth.assertThat(block.category).isEqualTo(OnrampOfferCategory.Recommended)
                    Truth.assertThat(block.offers).hasSize(1)
                    val offer = block.offers.single()
                    Truth.assertThat(offer.quote.provider.id).isEqualTo("provider1")
                    Truth.assertThat(offer.advantages).isEqualTo(OnrampOfferAdvantages.Default)
                    Truth.assertThat(block.hasMoreOffers).isTrue()
                },
            )
        }
    }

    @Test
    fun `GIVEN restricted quote has best rate WHEN invoke THEN great rate goes to purchasable offer`() = runTest {
        // Arrange
        val paymentMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val quotes = listOf(
            createRestrictedQuote(paymentMethod, createMockProvider("restricted", "Restricted"), BigDecimal("130.0")),
            createMockQuote(paymentMethod, createMockProvider("best", "Best"), BigDecimal("100.0")),
            createMockQuote(paymentMethod, createMockProvider("cheap", "Cheap"), BigDecimal("90.0")),
        )
        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(emptyList())

        // Act
        val result = useCase()

        // Assert
        result.collect { either ->
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    val block = offers.single()
                    Truth.assertThat(block.category).isEqualTo(OnrampOfferCategory.Recommended)
                    val offer = block.offers.single()
                    Truth.assertThat(offer.quote.provider.id).isEqualTo("best")
                    Truth.assertThat(offer.advantages).isEqualTo(OnrampOfferAdvantages.GreatRate)
                    Truth.assertThat(block.hasMoreOffers).isTrue()
                },
            )
        }
    }

    @Test
    fun `GIVEN restricted instant quote WHEN invoke THEN fastest goes to purchasable offer`() = runTest {
        // Arrange
        val instantMethod = createMockPaymentMethod("google-pay", "Google Pay", PaymentMethodType.GOOGLE_PAY)
        val purchasableInstantMethod = createMockPaymentMethod("revolut", "Revolut Pay", PaymentMethodType.REVOLUT_PAY)
        val slowMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val quotes = listOf(
            createRestrictedQuote(instantMethod, createMockProvider("restricted", "Restricted"), BigDecimal("130.0")),
            createMockQuote(slowMethod, createMockProvider("best", "Best"), BigDecimal("100.0")),
            createMockQuote(purchasableInstantMethod, createMockProvider("fast", "Fast"), BigDecimal("90.0")),
        )
        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(emptyList())

        // Act
        val result = useCase()

        // Assert
        result.collect { either ->
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    val block = offers.single()
                    Truth.assertThat(block.category).isEqualTo(OnrampOfferCategory.Recommended)
                    Truth.assertThat(block.offers.map { it.quote.provider.id }).containsExactly("best", "fast")

                    val greatRateOffer = block.offers.first { it.advantages == OnrampOfferAdvantages.GreatRate }
                    Truth.assertThat(greatRateOffer.quote.provider.id).isEqualTo("best")

                    val fastestOffer = block.offers.first { it.advantages == OnrampOfferAdvantages.Fastest }
                    Truth.assertThat(fastestOffer.quote.provider.id).isEqualTo("fast")
                    // The rate baseline is the best purchasable rate (100), not the restricted 130
                    Truth.assertThat(fastestOffer.rateDif?.compareTo(BigDecimal("0.1"))).isEqualTo(0)
                },
            )
        }
    }

    @Test
    fun `GIVEN purchasable recent quote and restricted quote WHEN invoke THEN recent block kept alongside`() = runTest {
        // Arrange — the restricted quote is neither recent nor recommended, but still counts as "more offers"
        val cardMethod = createMockPaymentMethod("card", "Card", PaymentMethodType.CARD)
        val instantMethod = createMockPaymentMethod("google-pay", "Google Pay", PaymentMethodType.GOOGLE_PAY)
        val recentProvider = createMockProvider("recent", "Recent Provider")
        val quotes = listOf(
            createMockQuote(cardMethod, recentProvider, BigDecimal("100.0")),
            createMockQuote(instantMethod, createMockProvider("fast", "Fast"), BigDecimal("90.0")),
            createRestrictedQuote(cardMethod, createMockProvider("restricted", "Restricted"), BigDecimal("130.0")),
        )
        val transactions = listOf(createMockTransaction("Recent Provider", "Card", 1000L))
        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(transactions)

        // Act
        val result = useCase()

        // Assert
        result.collect { either ->
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers ->
                    val recentBlock = offers.find { it.category == OnrampOfferCategory.Recent }
                    Truth.assertThat(recentBlock).isNotNull()
                    Truth.assertThat(recentBlock?.offers?.single()?.quote?.provider?.id).isEqualTo("recent")
                    // recent is also the best purchasable rate, so it keeps the GreatRate advantage
                    Truth.assertThat(recentBlock?.offers?.single()?.advantages)
                        .isEqualTo(OnrampOfferAdvantages.GreatRate)

                    val providerIds = offers.flatMap { block -> block.offers.map { it.quote.provider.id } }
                    Truth.assertThat(providerIds).doesNotContain("restricted")
                    // 3 quotes, 2 shown (recent + fastest) → the restricted one is behind "All offers"
                    Truth.assertThat(offers.all { it.hasMoreOffers }).isTrue()
                },
            )
        }
    }

    @Test
    fun `GIVEN only amount error quotes WHEN invoke THEN returns empty list`() = runTest {
        // Arrange
        val quotes = listOf<OnrampQuote>(mockk<OnrampQuote.AmountError>())
        coEvery { settingsRepository.isGooglePayAvailability() } returns false
        coEvery { onrampRepository.getQuotes() } returns flowOf(quotes)
        coEvery { onrampTransactionRepository.getAllTransactions() } returns flowOf(emptyList())

        // Act
        val result = useCase()

        // Assert
        result.collect { either ->
            either.fold(
                ifLeft = { error -> Truth.assertThat(error).isNull() },
                ifRight = { offers -> Truth.assertThat(offers).isEmpty() },
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
        isRestricted: Boolean = false,
    ): OnrampQuote.Data {
        return mockk<OnrampQuote.Data> {
            every { this@mockk.paymentMethod } returns paymentMethod
            every { this@mockk.provider } returns provider
            every { this@mockk.isRestricted } returns isRestricted
            every { this@mockk.toAmount } returns mockk {
                every { value } returns toAmount
            }
        }
    }

    private fun createRestrictedQuote(
        paymentMethod: OnrampPaymentMethod,
        provider: OnrampProvider,
        rate: BigDecimal = BigDecimal("120.0"),
    ): OnrampQuote.Data {
        return createMockQuote(
            paymentMethod = paymentMethod,
            provider = provider,
            toAmount = rate,
            isRestricted = true,
        )
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