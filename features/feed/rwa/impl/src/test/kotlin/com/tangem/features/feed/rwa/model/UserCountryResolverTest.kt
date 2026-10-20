package com.tangem.features.feed.rwa.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.UserCountryError
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class UserCountryResolverTest {

    private val getUserCountryUseCase: GetUserCountryUseCase = mockk()

    private val resolver = UserCountryResolver(getUserCountryUseCase)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getUserCountryUseCase)
    }

    @Test
    fun `GIVEN a published country WHEN resolve THEN it is returned upper case`() = runTest {
        // Arrange
        every { getUserCountryUseCase.invoke() } returns flowOf(Either.Right(UserCountry.Other(code = "de")))

        // Act
        val actual = resolver.resolve()

        // Assert
        assertThat(actual).isEqualTo("DE")
    }

    @Test
    fun `GIVEN the country arrives after 10 s WHEN resolve THEN it waits for the first resolved country`() =
        runTest {
            // Arrange
            val countries = MutableSharedFlow<Either<UserCountryError, UserCountry>>()
            every { getUserCountryUseCase.invoke() } returns countries

            // Act
            var actual: String? = null
            val job = launch { actual = resolver.resolve() }
            advanceTimeBy(delayTimeMillis = 10_000)
            countries.emit(Either.Left(UserCountryError.NotSetup))
            countries.emit(Either.Right(UserCountry.Other(code = "gb")))
            advanceUntilIdle()
            job.join()

            // Assert
            assertThat(actual).isEqualTo("GB")
        }

    @Test
    fun `GIVEN a blank published country WHEN resolve THEN it counts as unknown`() = runTest {
        // Arrange
        every { getUserCountryUseCase.invoke() } returns flowOf(Either.Right(UserCountry.Other(code = "")))

        // Act
        val actual = resolver.resolve()

        // Assert
        assertThat(actual).isNull()
    }
}