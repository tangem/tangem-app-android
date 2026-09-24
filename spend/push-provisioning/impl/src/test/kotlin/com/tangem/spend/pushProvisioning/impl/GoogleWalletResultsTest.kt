package com.tangem.spend.pushProvisioning.impl

import arrow.core.left
import arrow.core.right
import com.google.android.gms.tapandpay.TapAndPayStatusCodes
import com.google.android.gms.tapandpay.issuer.TokenInfo
import com.google.common.truth.Truth.assertThat
import com.meawallet.mpp.GooglePayTokenState
import com.meawallet.mpp.MppError
import com.meawallet.mpp.MppErrorCode
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.CardStatus
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.FailureReason
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class GoogleWalletResultsTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToCardStatus {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN wallet tokens WHEN toCardStatus THEN aggregated status returned`(model: CardStatusModel) {
            // Act
            val actual = model.tokenStates.toCardStatus()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CardStatusModel(tokenStates = emptyList(), expected = CardStatus.NotAdded),
            CardStatusModel(
                tokenStates = listOf(GooglePayTokenState.TOKEN_STATE_NOT_FOUND),
                expected = CardStatus.NotAdded,
            ),
            CardStatusModel(
                tokenStates = listOf(GooglePayTokenState.TOKEN_STATE_UNTOKENIZED),
                expected = CardStatus.NotAdded,
            ),
            CardStatusModel(
                tokenStates = listOf(GooglePayTokenState.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION),
                expected = CardStatus.Incomplete,
            ),
            CardStatusModel(
                tokenStates = listOf(GooglePayTokenState.TOKEN_STATE_ACTIVE),
                expected = CardStatus.Added,
            ),
            CardStatusModel(
                tokenStates = listOf(GooglePayTokenState.TOKEN_STATE_PENDING),
                expected = CardStatus.Added,
            ),
            CardStatusModel(
                tokenStates = listOf(GooglePayTokenState.TOKEN_STATE_SUSPENDED),
                expected = CardStatus.Added,
            ),
            CardStatusModel(
                tokenStates = listOf(GooglePayTokenState.TOKEN_STATE_FELICA_PENDING_PROVISIONING),
                expected = CardStatus.Added,
            ),
            CardStatusModel(
                tokenStates = listOf(
                    GooglePayTokenState.TOKEN_STATE_UNTOKENIZED,
                    GooglePayTokenState.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION,
                ),
                expected = CardStatus.Incomplete,
            ),
            CardStatusModel(
                tokenStates = listOf(
                    GooglePayTokenState.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION,
                    GooglePayTokenState.TOKEN_STATE_ACTIVE,
                ),
                expected = CardStatus.Added,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToTokenLookup {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN lookup error WHEN toTokenLookup THEN missing card is empty and other errors fail`(
            model: TokenLookupModel,
        ) {
            // Act
            val actual = model.error.toTokenLookup()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<TokenLookupModel> {
            val notAllowlistedError = createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE)
            val attestationError = createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_ATTESTATION_ERROR)
            val hardwareIdError = FakeMppError(errorCode = MppErrorCode.STABLE_HARDWARE_ID_ERROR)

            return listOf(
                TokenLookupModel(
                    error = createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET),
                    expected = emptyList<TokenInfo>().right(),
                ),
                TokenLookupModel(
                    error = createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_TOKEN_NOT_FOUND),
                    expected = emptyList<TokenInfo>().right(),
                ),
                TokenLookupModel(
                    error = notAllowlistedError,
                    expected = TokenLookupFailure(
                        isWalletUnsupported = true,
                        description = notAllowlistedError.describe(),
                        sdkErrorCode = MppErrorCode.INTERNAL_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE,
                    ).left(),
                ),
                TokenLookupModel(
                    error = attestationError,
                    expected = TokenLookupFailure(
                        isWalletUnsupported = false,
                        description = attestationError.describe(),
                        sdkErrorCode = MppErrorCode.INTERNAL_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_ATTESTATION_ERROR,
                    ).left(),
                ),
                TokenLookupModel(
                    error = hardwareIdError,
                    expected = TokenLookupFailure(
                        isWalletUnsupported = false,
                        description = hardwareIdError.describe(),
                        sdkErrorCode = MppErrorCode.STABLE_HARDWARE_ID_ERROR,
                    ).left(),
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ThrowableToTokenLookup {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN synchronous exception WHEN toTokenLookup THEN lookup fails`(model: ThrowableLookupModel) {
            // Act
            val actual = model.throwable.toTokenLookup()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<ThrowableLookupModel> {
            val notAllowlistedError = createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE)

            return listOf(
                ThrowableLookupModel(
                    throwable = createMppException(notAllowlistedError),
                    expected = TokenLookupFailure(
                        isWalletUnsupported = true,
                        description = notAllowlistedError.describe(),
                        sdkErrorCode = MppErrorCode.INTERNAL_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE,
                    ).left(),
                ),
                ThrowableLookupModel(
                    throwable = IllegalStateException("Unexpected"),
                    expected = TokenLookupFailure(
                        isWalletUnsupported = false,
                        description = "IllegalStateException: Unexpected",
                    ).left(),
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TokenLookupFailureToAddResult {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN token lookup failure WHEN toAddResult THEN failure keeps its reason and codes`(
            model: LookupFailureModel,
        ) {
            // Act
            val actual = model.failure.toAddResult()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            LookupFailureModel(
                failure = TokenLookupFailure(
                    isWalletUnsupported = true,
                    description = "not allowlisted",
                    sdkErrorCode = MppErrorCode.INTERNAL_ERROR,
                    walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE,
                ),
                expected = AddResult.Failed(
                    reason = FailureReason.UNSUPPORTED,
                    description = "not allowlisted",
                    sdkErrorCode = MppErrorCode.INTERNAL_ERROR,
                    walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE,
                ),
            ),
            LookupFailureModel(
                failure = TokenLookupFailure(isWalletUnsupported = false, description = "Token lookup timed out"),
                expected = AddResult.Failed(
                    reason = FailureReason.TOKEN_LOOKUP_FAILED,
                    description = "Token lookup timed out",
                ),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToAddResult {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN provisioning error WHEN toAddResult THEN result mapped`(model: AddResultModel) {
            // Act
            val actual = model.error.toAddResult()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<AddResultModel> {
            val declinedError = createDeclinedError()
            val googlePayError = FakeMppError(errorCode = MppErrorCode.GOOGLE_PAY_ERROR)
            val walletApiError = createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_ATTESTATION_ERROR)
            val notAllowlistedError = FakeMppError(
                errorCode = MppErrorCode.STABLE_HARDWARE_ID_ERROR,
                walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE,
            )
            val debuggerError = FakeMppError(errorCode = MppErrorCode.DEBUGGER_ATTACHED)
            val networkErrors = listOf(
                MppErrorCode.NO_NETWORK_CONNECTION,
                MppErrorCode.GENERIC_NETWORK_PROBLEM,
                MppErrorCode.NETWORK_TIMEOUT,
                MppErrorCode.UNKNOWN_HOST,
                MppErrorCode.SSL_HANDSHAKE_FAILED,
                MppErrorCode.REMOTE_GATEWAY_TIMEOUT_ERROR,
            ).map { code -> FakeMppError(errorCode = code) }

            return listOf(
                AddResultModel(
                    error = FakeMppError(errorCode = MppErrorCode.OPERATION_CANCELLED_BY_USER),
                    expected = AddResult.Cancelled,
                ),
                AddResultModel(
                    error = FakeMppError(
                        errorCode = MppErrorCode.GOOGLE_PAY_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_USER_CANCELED_FLOW,
                    ),
                    expected = AddResult.Cancelled,
                ),
                AddResultModel(
                    error = FakeMppError(errorCode = MppErrorCode.GOOGLE_PAY_TOKEN_ALREADY_EXISTS),
                    expected = AddResult.AlreadyAdded,
                ),
                AddResultModel(
                    error = declinedError,
                    expected = AddResult.Failed(
                        reason = FailureReason.GOOGLE_WALLET_ERROR,
                        description = declinedError.describe(),
                        sdkErrorCode = MppErrorCode.GOOGLE_PAY_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_TOKENIZATION_DECLINED,
                    ),
                ),
                AddResultModel(
                    error = googlePayError,
                    expected = AddResult.Failed(
                        reason = FailureReason.GOOGLE_WALLET_ERROR,
                        description = googlePayError.describe(),
                        sdkErrorCode = MppErrorCode.GOOGLE_PAY_ERROR,
                    ),
                ),
                AddResultModel(
                    error = walletApiError,
                    expected = AddResult.Failed(
                        reason = FailureReason.GOOGLE_WALLET_ERROR,
                        description = walletApiError.describe(),
                        sdkErrorCode = MppErrorCode.INTERNAL_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_ATTESTATION_ERROR,
                    ),
                ),
                AddResultModel(
                    error = notAllowlistedError,
                    expected = AddResult.Failed(
                        reason = FailureReason.UNSUPPORTED,
                        description = notAllowlistedError.describe(),
                        sdkErrorCode = MppErrorCode.STABLE_HARDWARE_ID_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE,
                    ),
                ),
                AddResultModel(
                    error = debuggerError,
                    expected = AddResult.Failed(
                        reason = FailureReason.MEAWALLET_ERROR,
                        description = debuggerError.describe(),
                        sdkErrorCode = MppErrorCode.DEBUGGER_ATTACHED,
                    ),
                ),
            ) + networkErrors.map { error ->
                AddResultModel(
                    error = error,
                    expected = AddResult.Failed(
                        reason = FailureReason.NETWORK_ERROR,
                        description = error.describe(),
                        sdkErrorCode = error.code,
                    ),
                )
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ThrowableToAddResult {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN synchronous exception WHEN toAddResult THEN result mapped`(model: ThrowableModel) {
            // Act
            val actual = model.throwable.toAddResult()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<ThrowableModel> {
            val alreadyExistsError = FakeMppError(errorCode = MppErrorCode.GOOGLE_PAY_TOKEN_ALREADY_EXISTS)
            val notInitializedError = FakeMppError(errorCode = MppErrorCode.NOT_INITIALIZED)

            return listOf(
                ThrowableModel(throwable = createMppException(alreadyExistsError), expected = AddResult.AlreadyAdded),
                ThrowableModel(
                    throwable = createMppException(notInitializedError),
                    expected = AddResult.Failed(
                        reason = FailureReason.MEAWALLET_ERROR,
                        description = notInitializedError.describe(),
                        sdkErrorCode = MppErrorCode.NOT_INITIALIZED,
                    ),
                ),
                ThrowableModel(
                    throwable = IllegalStateException("Unexpected"),
                    expected = AddResult.Failed(
                        reason = FailureReason.UNEXPECTED_ERROR,
                        description = "IllegalStateException: Unexpected",
                    ),
                ),
            )
        }
    }

    @Nested
    inner class Describe {

        @Test
        fun `GIVEN SDK error report WHEN describe THEN report flattened into one line`() {
            // Arrange
            val error = createDeclinedError(trace = "trace-id")

            // Act
            val actual = error.describe()

            // Assert
            assertThat(actual).isEqualTo(
                "MppError[ code: FAKE_ERROR (701) message: Fake error traceId: trace-id Wallet error code: 15022 ]",
            )
        }
    }

    internal data class CardStatusModel(val tokenStates: List<GooglePayTokenState>, val expected: CardStatus)

    internal data class TokenLookupModel(val error: MppError, val expected: TokenLookup)

    internal data class ThrowableLookupModel(val throwable: Throwable, val expected: TokenLookup)

    internal data class LookupFailureModel(val failure: TokenLookupFailure, val expected: AddResult.Failed)

    internal data class AddResultModel(val error: MppError, val expected: AddResult)

    internal data class ThrowableModel(val throwable: Throwable, val expected: AddResult)
}