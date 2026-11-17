package com.tangem.domain.onramp

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class GetOnrampCountryByIpUseCaseTest {

    private val repository: OnrampRepository = mockk()
    private val errorResolver: OnrampErrorResolver = mockk()
    private val userWallet: UserWallet = mockk()
    private val useCase = GetOnrampCountryByIpUseCase(repository, errorResolver)

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `GIVEN fresh country WHEN invoke THEN fresh result wins without reading cache`(isOnrampAvailable: Boolean) = runTest {
        // Arrange
        val freshCountry = createCountry(isOnrampAvailable = isOnrampAvailable)
        coEvery { repository.getCountryByIp(userWallet) } returns freshCountry

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isEqualTo(freshCountry.right())
        coVerify(exactly = 1) { repository.getCountryByIp(userWallet, fromCache = false) }
        coVerify(exactly = 0) { repository.getCachedCountryByIp() }
    }

    @Test
    fun `GIVEN restricted country cached WHEN fresh lookup fails THEN cached restriction is returned`() = runTest {
        // Arrange
        val cachedCountry = createCountry(isOnrampAvailable = false)
        coEvery { repository.getCountryByIp(userWallet) } throws IllegalStateException("Offline")
        coEvery { repository.getCachedCountryByIp() } returns cachedCountry

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isEqualTo(cachedCountry.right())
        coVerify(exactly = 1) { repository.getCachedCountryByIp() }
        coVerify(exactly = 0) { repository.getDefaultCountrySync() }
        verify(exactly = 0) { errorResolver.resolve(any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `GIVEN no restricted cache WHEN lookup fails THEN original error is returned without retry`(
        hasCachedCountry: Boolean,
    ) = runTest {
        // Arrange
        val failure = IllegalStateException("No country")
        val error = OnrampError.DomainError("No country")
        coEvery { repository.getCountryByIp(userWallet) } throws failure
        coEvery { repository.getCachedCountryByIp() } returns
            createCountry(isOnrampAvailable = true).takeIf { hasCachedCountry }
        every { errorResolver.resolve(failure) } returns error

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isEqualTo(error.left())
        coVerify(exactly = 1) { repository.getCountryByIp(userWallet, any()) }
        coVerify(exactly = 1) { repository.getCachedCountryByIp() }
        verify(exactly = 1) { errorResolver.resolve(failure) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `GIVEN cancelled lookup WHEN invoke THEN cancellation propagates`(cancelFallback: Boolean) = runTest {
        // Arrange
        val cancellation = CancellationException("Cancelled")
        coEvery { repository.getCountryByIp(userWallet) } throws
            if (cancelFallback) IllegalStateException("Offline") else cancellation
        coEvery { repository.getCachedCountryByIp() } throws cancellation

        // Act
        val actual = runCatching { useCase(userWallet) }.exceptionOrNull()

        // Assert
        assertThat(actual).isSameInstanceAs(cancellation)
        coVerify(exactly = if (cancelFallback) 1 else 0) {
            repository.getCachedCountryByIp()
        }
        verify(exactly = 0) { errorResolver.resolve(any()) }
    }

    private fun createCountry(isOnrampAvailable: Boolean = false) = OnrampCountry(
        id = "gb",
        name = "United Kingdom",
        code = "GB",
        image = "",
        alpha3 = "GBR",
        continent = "Europe",
        defaultCurrency = OnrampCurrency(name = "Pound", code = "GBP", image = null, precision = 2, unit = "£"),
        isOnrampAvailable = isOnrampAvailable,
    )
}