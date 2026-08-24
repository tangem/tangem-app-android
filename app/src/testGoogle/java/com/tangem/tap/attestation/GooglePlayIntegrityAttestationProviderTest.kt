package com.tangem.tap.attestation

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.tangem.google.GoogleServicesHelper
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GooglePlayIntegrityAttestationProviderTest {

    private val context: Context = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val provider = GooglePlayIntegrityAttestationProvider(
        context = context,
        dispatchers = dispatchers,
    )

    @AfterEach
    fun teardown() = unmockkAll()

    @Test
    fun `GIVEN Google Play services unavailable WHEN getAttestationToken THEN returns null`() = runTest {
        // Arrange
        mockkObject(GoogleServicesHelper)
        every { GoogleServicesHelper.checkGoogleServicesAvailability(any()) } returns false

        // Act
        val token = provider.getAttestationToken("aGVsbG8td29ybGQtdGVzdC1ub25jZQ")

        // Assert
        assertThat(token).isNull()
    }

    @Test
    fun `GIVEN cloud project number unavailable WHEN getAttestationToken THEN returns null`() = runTest {
        // Arrange
        mockkObject(GoogleServicesHelper)
        every { GoogleServicesHelper.checkGoogleServicesAvailability(any()) } returns true
        mockkStatic(FirebaseApp::class)
        val options = mockk<FirebaseOptions> { every { gcmSenderId } returns null }
        every { FirebaseApp.getInstance() } returns mockk { every { this@mockk.options } returns options }

        // Act
        val token = provider.getAttestationToken("aGVsbG8td29ybGQtdGVzdC1ub25jZQ")

        // Assert
        assertThat(token).isNull()
    }
}