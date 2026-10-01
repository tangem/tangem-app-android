package com.tangem.features.introduction.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.hotwallet.SocialLoginConsent
import com.tangem.features.introduction.impl.ui.state.SocialLoginConsentUM
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SocialLoginConsentPromptTest {

    private val states = mutableListOf<SocialLoginConsentUM?>()

    private val prompt = SocialLoginConsentPrompt(
        title = stringReference("title"),
        description = stringReference("description"),
        acknowledgement = stringReference("acknowledgement"),
        onStateChanged = { states += it },
    )

    private val current: SocialLoginConsentUM get() = states.last()!!

    @Test
    fun `GIVEN both boxes ticked WHEN confirming THEN the opt-in is reported`() = runTest {
        // Arrange
        val answer = async { prompt.await() }
        runCurrent()

        // Act
        current.onAcknowledgedChange(true)
        current.onEmailSharingChange(true)
        current.onConfirmClick()

        // Assert
        assertThat(answer.await()).isEqualTo(SocialLoginConsent(isEmailSharingAllowed = true))
        assertThat(states.last()).isNull()
    }

    @Test
    fun `GIVEN only the acknowledgement WHEN confirming THEN no opt-in is reported`() = runTest {
        // Arrange
        val answer = async { prompt.await() }
        runCurrent()

        // Act
        current.onAcknowledgedChange(true)
        current.onConfirmClick()

        // Assert
        assertThat(answer.await()).isEqualTo(SocialLoginConsent(isEmailSharingAllowed = false))
    }

    @Test
    fun `GIVEN the acknowledgement is missing WHEN confirming THEN the prompt stays open and points at it`() =
        runTest {
            // Arrange
            val answer = async { prompt.await() }
            runCurrent()

            // Act
            current.onConfirmClick()

            // Assert
            assertThat(current.isAcknowledgementMissing).isTrue()
            assertThat(states.last()).isNotNull()

            current.onCloseClick()
            assertThat(answer.await()).isNull()
        }

    @Test
    fun `GIVEN the hint is on WHEN the box is ticked THEN the hint goes away`() = runTest {
        // Arrange
        val answer = async { prompt.await() }
        runCurrent()
        current.onConfirmClick()

        // Act
        current.onAcknowledgedChange(true)

        // Assert
        assertThat(current.isAcknowledgementMissing).isFalse()

        current.onCloseClick()
        answer.await()
    }

    @Test
    fun `GIVEN the box is ticked WHEN it is unticked THEN the hint is shown`() = runTest {
        // Arrange
        val answer = async { prompt.await() }
        runCurrent()
        current.onAcknowledgedChange(true)

        // Act
        current.onAcknowledgedChange(false)

        // Assert
        assertThat(current.isAcknowledgementMissing).isTrue()

        current.onCloseClick()
        answer.await()
    }

    @Test
    fun `GIVEN the screen is closed WHEN awaiting THEN nothing is agreed to`() = runTest {
        // Arrange
        val answer = async { prompt.await() }
        runCurrent()

        // Act
        current.onCloseClick()

        // Assert
        assertThat(answer.await()).isNull()
        assertThat(states.last()).isNull()
    }
}