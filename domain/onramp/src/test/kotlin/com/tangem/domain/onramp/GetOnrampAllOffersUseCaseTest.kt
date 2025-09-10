package com.tangem.domain.onramp

import com.google.common.truth.Truth
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampOfferAdvantages
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProvider
import com.tangem.domain.onramp.model.OnrampQuote
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
    fun `invoke should return empty list when no valid quotes`() = runTest {
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
    fun `invoke should return grouped offers with best rate marked`() = runTest {
        val paymentMethod1 = createMockPaymentMethod("card", "Card")
        val paymentMethod2 = createMockPaymentMethod("bank", "Bank Transfer")
        val provider1 = createMockProvider("provider1", "Provider 1")
        val provider2 = createMockProvider("provider2", "Provider 2")

        val quotes = listOf(
            createMockQuote(paymentMethod1, provider1, BigDecimal("100.0")),
            createMockQuote(paymentMethod1, provider2, BigDecimal("95.0")),
            createMockQuote(paymentMethod2, provider1, BigDecimal("98.0")),
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

                    val bestRateOffer = cardGroup?.offers?.find { it.advantages == OnrampOfferAdvantages.BestRate }
                    Truth.assertThat(bestRateOffer).isNotNull()

                    val bankGroup = offers.find { it.paymentMethod.id == "bank" }
                    Truth.assertThat(bankGroup).isNotNull()
                    Truth.assertThat(bankGroup?.offers).hasSize(1)
                    Truth.assertThat(bankGroup?.providerCount).isEqualTo(1)
                    Truth.assertThat(bankGroup?.isBestPaymentMethod).isFalse()
                },
            )
        }

        coVerify { onrampRepository.getQuotes() }
        coVerify { settingsRepository.isGooglePayAvailability() }
    }

    @Test
    fun `invoke should sort offers by toAmount descending`() = runTest {
        val paymentMethod = createMockPaymentMethod("card", "Card")
        val provider = createMockProvider("provider1", "Provider 1")

        val quotes = listOf(
            createMockQuote(paymentMethod, provider, BigDecimal("90.0")),
            createMockQuote(paymentMethod, provider, BigDecimal("100.0")),
            createMockQuote(paymentMethod, provider, BigDecimal("95.0")),
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

    private fun createMockPaymentMethod(id: String, name: String): OnrampPaymentMethod {
        return mockk<OnrampPaymentMethod> {
            every { this@mockk.id } returns id
            every { this@mockk.name } returns name
            every { this@mockk.type } returns mockk {
                every { getPriority(any()) } returns 1
            }
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
}