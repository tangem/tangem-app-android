package com.tangem.features.tangempay.card.details

import com.google.common.truth.Truth.assertThat
import com.tangem.core.biometric.BiometricAuthError
import com.tangem.core.biometric.BiometricAuthManager
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.features.tangempay.details.impl.R
import com.tangem.test.core.ProvideTestModels
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import com.tangem.core.ui.R as CoreUiR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCardAuthenticatorTest {

    private val biometricAuthManager: BiometricAuthManager = mockk()
    private val uiMessageSender: UiMessageSender = mockk()
    private val settingsManager: SettingsManager = mockk()
    private val sentMessages = mutableListOf<UiMessage>()

    private val authenticator = TangemPayCardAuthenticator(
        biometricAuthManager = biometricAuthManager,
        uiMessageSender = uiMessageSender,
        settingsManager = settingsManager,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(biometricAuthManager, uiMessageSender, settingsManager)
        sentMessages.clear()
        every { uiMessageSender.send(capture(sentMessages)) } just Runs
    }

    @Test
    fun `GIVEN user authenticated WHEN authenticate THEN returns true without messages`() = runTest {
        // Arrange
        coEvery { biometricAuthManager.authenticate(any()) } returns BiometricAuthManager.Result.Success

        // Act
        val actual = authenticator.authenticate(title = TITLE)

        // Assert
        assertThat(actual).isTrue()
        assertThat(sentMessages).isEmpty()
        coVerify(exactly = 1) {
            biometricAuthManager.authenticate(BiometricAuthManager.Config(title = TITLE, subtitle = null))
        }
    }

    @Test
    fun `GIVEN prompt cancelled WHEN authenticate THEN returns false without messages`() = runTest {
        // Arrange
        coEvery { biometricAuthManager.authenticate(any()) } returns BiometricAuthManager.Result.Cancelled

        // Act
        val actual = authenticator.authenticate(title = TITLE)

        // Assert
        assertThat(actual).isFalse()
        assertThat(sentMessages).isEmpty()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN authentication failed WHEN authenticate THEN returns false and shows error snackbar`(
        model: FailureModel,
    ) = runTest {
        // Arrange
        coEvery { biometricAuthManager.authenticate(any()) } returns failure(model.error)

        // Act
        val actual = authenticator.authenticate(title = TITLE)

        // Assert
        assertThat(actual).isFalse()
        assertThat(sentMessages).containsExactlyElementsIn(listOfNotNull(model.expectedMessage))
    }

    @ParameterizedTest
    @MethodSource("provideScreenLockNotSetErrors")
    fun `GIVEN screen lock not set WHEN authenticate THEN returns false and shows protection sheet`(
        error: BiometricAuthError,
    ) = runTest {
        // Arrange
        coEvery { biometricAuthManager.authenticate(any()) } returns failure(error)

        // Act
        val actual = authenticator.authenticate(title = TITLE)

        // Assert
        assertThat(actual).isFalse()
        assertThat(sentMessages.single()).isInstanceOf(BottomSheetMessage::class.java)
    }

    private fun failure(error: BiometricAuthError) = BiometricAuthManager.Result.Failure(error = error, message = null)

    private fun provideTestModels() = listOf(
        FailureModel(BiometricAuthError.HardwareUnavailable, snackbar(CoreUiR.string.common_unknown_error)),
        FailureModel(BiometricAuthError.NoForegroundActivity, snackbar(CoreUiR.string.common_unknown_error)),
        FailureModel(BiometricAuthError.Unknown, snackbar(CoreUiR.string.common_unknown_error)),
        FailureModel(
            BiometricAuthError.LockedOutPermanently,
            snackbar(CoreUiR.string.biometric_lockout_permanent_warning_title),
        ),
        FailureModel(
            BiometricAuthError.SecurityUpdateRequired,
            snackbar(CoreUiR.string.alert_authentication_error_message),
        ),
        FailureModel(BiometricAuthError.LockedOut),
        FailureModel(BiometricAuthError.SystemCancelled),
        FailureModel(BiometricAuthError.Timeout),
    )

    private fun provideScreenLockNotSetErrors() = listOf(
        BiometricAuthError.NoBiometricEnrolled,
        BiometricAuthError.NoDeviceCredential,
    )

    private fun snackbar(id: Int) = SnackbarMessage(resourceReference(id))

    internal data class FailureModel(
        val error: BiometricAuthError,
        val expectedMessage: SnackbarMessage? = null,
    )

    private companion object {
        val TITLE: TextReference = resourceReference(R.string.tangempay_card_details_title)
    }
}