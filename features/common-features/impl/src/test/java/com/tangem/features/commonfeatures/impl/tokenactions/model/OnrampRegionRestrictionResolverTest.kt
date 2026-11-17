package com.tangem.features.commonfeatures.impl.tokenactions.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.GetOnrampCountryByIpUseCase
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.featuretoggles.OnrampFeatureToggles
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OnrampRegionRestrictionResolverTest {

    private val getOnrampCountryByIpUseCase: GetOnrampCountryByIpUseCase = mockk()
    private val onrampFeatureToggles: OnrampFeatureToggles = mockk()
    private val userWallet: UserWallet = mockk()

    private val resolver = OnrampRegionRestrictionResolver(getOnrampCountryByIpUseCase, onrampFeatureToggles)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getOnrampCountryByIpUseCase, onrampFeatureToggles)
    }

    @Test
    fun `GIVEN IP country without onramp WHEN resolve THEN restriction names the country`() = runTest {
        // Arrange
        coEvery { getOnrampCountryByIpUseCase(userWallet) } returns createCountry(isOnrampAvailable = false).right()

        // Act
        val actual = resolver.resolve(userWallet)

        // Assert
        assertThat(actual).isEqualTo(OnrampRegionState.Restricted("United Kingdom"))
    }

    @Test
    fun `GIVEN IP country with onramp WHEN resolve THEN no restriction`() = runTest {
        // Arrange
        coEvery { getOnrampCountryByIpUseCase(userWallet) } returns createCountry(isOnrampAvailable = true).right()

        // Act
        val actual = resolver.resolve(userWallet)

        // Assert
        assertThat(actual).isEqualTo(OnrampRegionState.Allowed)
    }

    @Test
    fun `GIVEN country lookup failed WHEN resolve THEN region stays unknown`() = runTest {
        // Arrange
        val error: Either<OnrampError, OnrampCountry> = OnrampError.DomainError("error").left()
        coEvery { getOnrampCountryByIpUseCase(userWallet) } returns error

        // Act
        val actual = resolver.resolve(userWallet)

        // Assert
        assertThat(actual).isEqualTo(OnrampRegionState.Unknown)
    }

    private fun createCountry(isOnrampAvailable: Boolean) = OnrampCountry(
        id = "gb",
        name = "United Kingdom",
        code = "GB",
        image = "https://flag.png",
        alpha3 = "GBR",
        continent = "Europe",
        defaultCurrency = OnrampCurrency(name = "Pound", code = "GBP", image = null, precision = 2, unit = "£"),
        isOnrampAvailable = isOnrampAvailable,
    )
}