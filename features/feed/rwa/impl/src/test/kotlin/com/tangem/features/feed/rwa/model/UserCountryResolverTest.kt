package com.tangem.features.feed.rwa.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.UserCountryError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
internal class UserCountryResolverTest {

    private val getUserCountryUseCase: GetUserCountryUseCase = mockk()

    private val resolver = UserCountryResolver(getUserCountryUseCase)

    private val defaultLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun resetMocks() {
        clearMocks(getUserCountryUseCase)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN a stored country WHEN resolve THEN it is returned upper case without waiting`() = runTest {
        // Arrange
        every { getUserCountryUseCase.invokeSync() } returns Either.Right(UserCountry.Other(code = "de"))

        // Act
        val actual = resolver.resolve()

        // Assert
        assertThat(actual).isEqualTo("DE")
    }

    @Test
    fun `GIVEN the country arrives late WHEN resolve THEN it waits for the first emission`() = runTest {
        // Arrange
        every { getUserCountryUseCase.invokeSync() } returns Either.Left(UserCountryError.NotSetup)
        val countries = MutableSharedFlow<Either<UserCountryError, UserCountry>>()
        every { getUserCountryUseCase.invoke() } returns countries

        // Act
        var actual: String? = null
        val job = launch { actual = resolver.resolve() }
        advanceTimeBy(delayTimeMillis = 1_000)
        countries.emit(Either.Right(UserCountry.Other(code = "us")))
        advanceUntilIdle()
        job.join()

        // Assert
        assertThat(actual).isEqualTo("US")
    }

    @Test
    fun `GIVEN the country never arrives WHEN resolve THEN the device region stands in`() = runTest {
        // Arrange — answering without a country asks the backend to lift every restriction, so the
        // device region is a better last resort than giving up
        Locale.setDefault(Locale.UK)
        every { getUserCountryUseCase.invokeSync() } returns Either.Left(UserCountryError.NotSetup)
        every { getUserCountryUseCase.invoke() } returns MutableSharedFlow()

        // Act
        val actual = resolver.resolve()

        // Assert
        assertThat(actual).isEqualTo("GB")
    }

    @Test
    fun `GIVEN neither source knows the region WHEN resolve THEN nothing is returned`() = runTest {
        // Arrange
        Locale.setDefault(Locale("en"))
        every { getUserCountryUseCase.invokeSync() } returns Either.Left(UserCountryError.NotSetup)
        every { getUserCountryUseCase.invoke() } returns MutableSharedFlow()

        // Act
        val actual = resolver.resolve()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a blank stored country WHEN resolve THEN it counts as unknown`() = runTest {
        // Arrange — an empty geo answer must not travel as a country
        every { getUserCountryUseCase.invokeSync() } returns Either.Right(UserCountry.Other(code = ""))
        coEvery { getUserCountryUseCase.invoke() } returns flowOf(Either.Left(UserCountryError.NotSetup))

        // Act
        val actual = resolver.resolve()

        // Assert
        assertThat(actual).isNull()
    }
}