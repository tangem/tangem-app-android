package com.tangem.google.auth

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.google.GoogleServicesHelper
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CredentialManagerGoogleIdTokenProviderAvailabilityTest {

    private val context: Context = mockk()
    private val foregroundActivityProvider: ForegroundActivityProvider = mockk()

    @BeforeAll
    fun mockGoogleServicesHelper() {
        mockkObject(GoogleServicesHelper)
    }

    @AfterAll
    fun unmockGoogleServicesHelper() {
        unmockkObject(GoogleServicesHelper)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN device and environment WHEN isAvailable THEN offered only when sign-in can finish`(
        model: AvailabilityModel,
    ) = runTest {
        // Arrange
        every { GoogleServicesHelper.checkGoogleServicesAvailability(context) } returns model.hasGoogleServices
        val provider = createProvider(webClientId = model.webClientId)

        // Act
        val actual = provider.isAvailable()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun createProvider(webClientId: String?) = CredentialManagerGoogleIdTokenProvider(
        context = context,
        googleAuthConfig = GoogleAuthConfig(webClientId = webClientId),
        foregroundActivityProvider = foregroundActivityProvider,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    data class AvailabilityModel(
        val hasGoogleServices: Boolean,
        val webClientId: String?,
        val expected: Boolean,
    )

    private fun provideTestModels() = listOf(
        AvailabilityModel(hasGoogleServices = true, webClientId = WEB_CLIENT_ID, expected = true),
        AvailabilityModel(hasGoogleServices = false, webClientId = WEB_CLIENT_ID, expected = false),
        AvailabilityModel(hasGoogleServices = true, webClientId = null, expected = false),
        AvailabilityModel(hasGoogleServices = true, webClientId = "", expected = false),
    )

    private companion object {
        const val WEB_CLIENT_ID = "web-client-id.apps.googleusercontent.com"
    }
}