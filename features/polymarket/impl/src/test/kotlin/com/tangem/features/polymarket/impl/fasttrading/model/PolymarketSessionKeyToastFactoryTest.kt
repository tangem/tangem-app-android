package com.tangem.features.polymarket.impl.fasttrading.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationFailure
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.ui.R as CoreUiR

internal class PolymarketSessionKeyToastFactoryTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Outcome {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN finished outcome WHEN create THEN toast matches the request and result`(model: OutcomeModel) {
            // Act
            val actual = PolymarketSessionKeyToastFactory.create(model.outcome)

            // Assert
            assertThat(actual?.message).isEqualTo(model.expected?.messageId?.let(::resourceReference))
            assertThat(actual?.startIconId).isEqualTo(model.expected?.iconId)
            assertThat(actual?.isIconTinted).isEqualTo(model.expected?.isIconTinted)
        }

        private fun provideTestModels() = listOf(
            OutcomeModel(success(ACTIVATE), expected = null),
            OutcomeModel(success(REACTIVATE), expected = null),
            OutcomeModel(success(EXTEND), expected = null),
            OutcomeModel(success(PAUSE), expected = null),
            OutcomeModel(success(RESUME), expected = null),
            OutcomeModel(success(REVOKE), expected = SUCCESS_REVOKED),
            OutcomeModel(failure(ACTIVATE), error(R.string.prediction_session_key_toast_activation_failed)),
            OutcomeModel(failure(REACTIVATE), error(R.string.prediction_session_key_toast_reactivation_failed)),
            OutcomeModel(
                failure(REACTIVATE, isAfterRevocation = true),
                error(R.string.prediction_session_key_toast_reactivation_failed_revoked),
            ),
            OutcomeModel(failure(EXTEND), error(R.string.prediction_session_key_toast_extension_failed)),
            OutcomeModel(
                failure(EXTEND, isAfterRevocation = true),
                error(R.string.prediction_session_key_toast_extension_failed_revoked),
            ),
            OutcomeModel(failure(REVOKE), error(R.string.prediction_session_key_toast_revocation_failed)),
            OutcomeModel(failure(PAUSE), error(R.string.prediction_session_key_toast_pausing_failed)),
            OutcomeModel(failure(RESUME), error(R.string.prediction_session_key_toast_resuming_failed)),
            OutcomeModel(failure(ACTIVATE, error = CANCELLED), expected = null),
            OutcomeModel(failure(EXTEND, error = CANCELLED, isAfterRevocation = true), expected = null),
            OutcomeModel(failure(REVOKE, error = CANCELLED), expected = null),
            OutcomeModel(failure(ACTIVATE, error = IN_PROGRESS), BUSY),
            OutcomeModel(failure(REACTIVATE, error = IN_PROGRESS), BUSY),
            OutcomeModel(failure(EXTEND, error = IN_PROGRESS), BUSY),
            OutcomeModel(failure(REVOKE, error = IN_PROGRESS), BUSY),
            OutcomeModel(failure(PAUSE, error = BACKEND_BUSY), BUSY),
            OutcomeModel(failure(RESUME, error = BACKEND_BUSY), BUSY),
            OutcomeModel(failure(REVOKE, error = BACKEND_BUSY), BUSY),
            OutcomeModel(
                failure(EXTEND, error = BACKEND_BUSY, isAfterRevocation = true),
                error(R.string.prediction_session_key_toast_extension_failed_revoked),
            ),
            OutcomeModel(
                failure(REACTIVATE, error = IN_PROGRESS, isAfterRevocation = true),
                error(R.string.prediction_session_key_toast_reactivation_failed_revoked),
            ),
            OutcomeModel(
                failure(REVOKE, error = PolymarketSessionKeyLifecycleError.RevocationFailed(sessionKey = null)),
                error(R.string.prediction_session_key_toast_revocation_failed),
            ),
            OutcomeModel(
                failure(ACTIVATE, error = PolymarketSessionKeyLifecycleError.StaleSignature(cause = null)),
                error(R.string.prediction_session_key_toast_activation_failed),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Start {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN start result WHEN create THEN busy is toasted and started is not`(model: StartModel) {
            // Act
            val actual = PolymarketSessionKeyToastFactory.create(model.start)

            // Assert
            assertThat(actual?.message).isEqualTo(model.expected?.messageId?.let(::resourceReference))
            assertThat(actual?.startIconId).isEqualTo(model.expected?.iconId)
        }

        private fun provideTestModels() = listOf(
            StartModel(PolymarketSessionKeyOperationStart.Busy, BUSY),
            StartModel(PolymarketSessionKeyOperationStart.Started(id = 7L), expected = null),
        )
    }

    data class ExpectedToast(val messageId: Int, val iconId: Int, val isIconTinted: Boolean)

    data class OutcomeModel(val outcome: PolymarketSessionKeyOperationOutcome, val expected: ExpectedToast?)

    data class StartModel(val start: PolymarketSessionKeyOperationStart, val expected: ExpectedToast?)

    private companion object {
        const val ADDRESS = "0x1111111111111111111111111111111111111111"
        val ACTIVATE = PolymarketSessionKeyOperationRequest.Activate
        val REACTIVATE = PolymarketSessionKeyOperationRequest.Reactivate
        val EXTEND = PolymarketSessionKeyOperationRequest.Extend
        val REVOKE = PolymarketSessionKeyOperationRequest.Revoke(sessionKeyAddress = ADDRESS)
        val PAUSE = PolymarketSessionKeyOperationRequest.SetEnabled(sessionKeyAddress = ADDRESS, isEnabled = false)
        val RESUME = PolymarketSessionKeyOperationRequest.SetEnabled(sessionKeyAddress = ADDRESS, isEnabled = true)

        val CANCELLED = PolymarketSessionKeyLifecycleError.Failed(
            PolymarketSessionKeyError.SigningFailed(PolymarketSigningError.UserCancelled),
        )
        val IN_PROGRESS = PolymarketSessionKeyLifecycleError.OperationInProgress
        val BACKEND_BUSY = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.SessionKeyBusy)
        val GENERIC = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network)

        val SUCCESS_REVOKED = ExpectedToast(
            messageId = R.string.prediction_session_key_toast_revoked,
            iconId = CoreUiR.drawable.ic_success_20,
            isIconTinted = false,
        )
        val BUSY = error(R.string.prediction_session_key_toast_busy)

        fun error(messageId: Int) = ExpectedToast(
            messageId = messageId,
            iconId = CoreUiR.drawable.ic_alert_circle_24,
            isIconTinted = true,
        )

        fun success(request: PolymarketSessionKeyOperationRequest) = PolymarketSessionKeyOperationOutcome(
            id = 1L,
            request = request,
            result = mockk<PolymarketSessionKey>().right(),
        )

        fun failure(
            request: PolymarketSessionKeyOperationRequest,
            error: PolymarketSessionKeyLifecycleError = GENERIC,
            isAfterRevocation: Boolean = false,
        ) = PolymarketSessionKeyOperationOutcome(
            id = 1L,
            request = request,
            result = PolymarketSessionKeyOperationFailure(error = error, isAfterRevocation = isAfterRevocation).left(),
        )
    }
}