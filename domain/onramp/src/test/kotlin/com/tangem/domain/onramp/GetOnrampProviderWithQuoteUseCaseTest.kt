package com.tangem.domain.onramp

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProvider
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetOnrampProviderWithQuoteUseCaseTest {

    private val repository: OnrampRepository = mockk()
    private val resolver: OnrampErrorResolver = mockk()
    private val paymentMethod = mockk<OnrampPaymentMethod> { every { id } returns "card" }
    private val provider = mockk<OnrampProvider> {
        every { paymentMethods } returns listOf(paymentMethod)
    }
    private val useCase = GetOnrampProviderWithQuoteUseCase(repository, resolver)

    @Test
    fun `GIVEN matching quote WHEN getting providers THEN preserves amounts and payment method`() = runTest {
        // Arrange
        val quote = mockk<OnrampQuote.Data>(relaxed = true) {
            every { provider } returns this@GetOnrampProviderWithQuoteUseCaseTest.provider
            every { paymentMethod } returns this@GetOnrampProviderWithQuoteUseCaseTest.paymentMethod
        }
        coEvery { repository.getQuotesSync() } returns listOf(quote)
        val expected = OnrampProviderWithQuote.Data(
            provider = provider,
            paymentMethod = paymentMethod,
            toAmount = quote.toAmount,
            fromAmount = quote.fromAmount,
        )

        // Act
        val result = useCase(paymentMethod)

        // Assert
        assertThat(result).isEqualTo(Either.Right(listOf(expected)))
    }

    @Test
    fun `GIVEN amount error WHEN getting providers THEN exposes amount error`() = runTest {
        // Arrange
        val quote = mockk<OnrampQuote.AmountError> {
            every { provider } returns this@GetOnrampProviderWithQuoteUseCaseTest.provider
            every { paymentMethod } returns this@GetOnrampProviderWithQuoteUseCaseTest.paymentMethod
        }
        coEvery { repository.getQuotesSync() } returns listOf(quote)

        // Act
        val result = useCase(paymentMethod)

        // Assert
        assertThat(result).isEqualTo(
            Either.Right(listOf(OnrampProviderWithQuote.Unavailable.AmountError(provider, quote))),
        )
    }

    @Test
    fun `GIVEN generic quote error WHEN getting providers THEN omits provider`() = runTest {
        // Arrange
        val quote = mockk<OnrampQuote.Error> {
            every { provider } returns this@GetOnrampProviderWithQuoteUseCaseTest.provider
            every { paymentMethod } returns this@GetOnrampProviderWithQuoteUseCaseTest.paymentMethod
        }
        coEvery { repository.getQuotesSync() } returns listOf(quote)

        // Act
        val result = useCase(paymentMethod)

        // Assert
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `GIVEN another payment method WHEN getting providers THEN lists available alternatives`() = runTest {
        // Arrange
        val selected = mockk<OnrampPaymentMethod> { every { id } returns "sepa" }
        val quote = mockk<OnrampQuote.Data> {
            every { provider } returns this@GetOnrampProviderWithQuoteUseCaseTest.provider
            every { paymentMethod } returns this@GetOnrampProviderWithQuoteUseCaseTest.paymentMethod
        }
        coEvery { repository.getQuotesSync() } returns listOf(quote)

        // Act
        val result = useCase(selected)

        // Assert
        assertThat(result).isEqualTo(
            Either.Right(
                listOf(OnrampProviderWithQuote.Unavailable.NotSupportedPaymentMethod(provider, listOf(paymentMethod))),
            ),
        )
    }

    @Test
    fun `GIVEN missing quotes WHEN getting providers THEN maps validation error`() = runTest {
        // Arrange
        val error = OnrampError.DomainError("missing quotes")
        coEvery { repository.getQuotesSync() } returns null
        every { resolver.resolve(any()) } returns error

        // Act
        val result = useCase(paymentMethod)

        // Assert
        assertThat(result).isEqualTo(Either.Left(error))
    }
}