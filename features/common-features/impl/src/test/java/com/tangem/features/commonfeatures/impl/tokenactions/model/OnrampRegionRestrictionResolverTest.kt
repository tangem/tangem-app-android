package com.tangem.features.commonfeatures.impl.tokenactions.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.GetOnrampCountryByIpUseCase
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OnrampRegionRestrictionResolverTest {

    private val getOnrampCountryByIpUseCase: GetOnrampCountryByIpUseCase = mockk()
    private val featureTogglesManager: FeatureTogglesManager = mockk()
    private val userWallet: UserWallet = mockk()

    private val resolver = OnrampRegionRestrictionResolver(getOnrampCountryByIpUseCase, featureTogglesManager)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getOnrampCountryByIpUseCase, featureTogglesManager)
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18954_ONRAMP_COUNTRY_BY_IP_ENABLED)
        } returns true
    }

    @Test
    fun `GIVEN IP country without onramp WHEN resolve THEN its name is returned`() = runTest {
        // Arrange
        coEvery { getOnrampCountryByIpUseCase(userWallet) } returns createCountry(isOnrampAvailable = false).right()

        // Act
        val actual = resolver.resolveRestrictedRegionName(userWallet)

        // Assert
        assertThat(actual).isEqualTo("United Kingdom")
    }

    @Test
    fun `GIVEN IP country with onramp WHEN resolve THEN no restriction`() = runTest {
        // Arrange
        coEvery { getOnrampCountryByIpUseCase(userWallet) } returns createCountry(isOnrampAvailable = true).right()

        // Act
        val actual = resolver.resolveRestrictedRegionName(userWallet)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN country lookup failed WHEN resolve THEN no restriction`() = runTest {
        // Arrange
        val error: Either<OnrampError, OnrampCountry> = OnrampError.DomainError("error").left()
        coEvery { getOnrampCountryByIpUseCase(userWallet) } returns error

        // Act
        val actual = resolver.resolveRestrictedRegionName(userWallet)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN toggle disabled WHEN resolve THEN country is not requested`() = runTest {
        // Arrange
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18954_ONRAMP_COUNTRY_BY_IP_ENABLED)
        } returns false

        // Act
        val actual = resolver.resolveRestrictedRegionName(userWallet)

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { getOnrampCountryByIpUseCase(any()) }
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