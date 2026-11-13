package com.tangem.features.polymarket.impl.onboarding.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketOnboardingFailureMessageTest {

    @ParameterizedTest
    @ProvideTestModels
    fun toFailureMessage(model: FailureMessageModel) {
        // Act
        val actual = model.error.toFailureMessage()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class FailureMessageModel(val error: PolymarketOnboardingError, val expected: TextReference?)

    private fun provideTestModels() = listOf(
        wallet(PolymarketWalletError.InvalidRequest, R.string.prediction_onboarding_error_request_declined),
        wallet(PolymarketWalletError.Unauthorized, R.string.prediction_onboarding_error_request_declined),
        wallet(
            PolymarketWalletError.RelayerRejected.NonceReused,
            R.string.prediction_onboarding_error_transaction_rejected,
        ),
        wallet(
            PolymarketWalletError.RelayerRejected.Other(detail = null),
            R.string.prediction_onboarding_error_transaction_rejected,
        ),
        wallet(PolymarketWalletError.RelayerUnavailable, R.string.prediction_onboarding_error_polymarket_unavailable),
        wallet(PolymarketWalletError.WalletNotDeployed, R.string.common_unknown_error),
        wallet(PolymarketWalletError.Network, R.string.common_unknown_error),
        wallet(PolymarketWalletError.Unexpected(httpCode = 500, detail = null), R.string.common_unknown_error),
        signing(PolymarketSigningError.NotDerived, R.string.prediction_onboarding_error_wallet_unsupported),
        signing(PolymarketSigningError.MissingWallet, R.string.prediction_onboarding_error_wallet_unsupported),
        signing(PolymarketSigningError.DerivationUnsupported, R.string.prediction_onboarding_error_wallet_unsupported),
        signing(PolymarketSigningError.CardError, R.string.prediction_onboarding_error_card_failed),
        signing(PolymarketSigningError.Unknown, R.string.common_unknown_error),
        signing(
            PolymarketSigningError.SessionKey(PolymarketSessionKeyError.KeyMissing),
            R.string.common_unknown_error,
        ),
        FailureMessageModel(
            error = PolymarketOnboardingError.Signing(PolymarketSigningError.UserCancelled),
            expected = null,
        ),
        FailureMessageModel(
            error = PolymarketOnboardingError.Auth(PolymarketAuthError.InvalidSignature),
            expected = resourceReference(R.string.prediction_onboarding_error_credentials),
        ),
        FailureMessageModel(
            error = PolymarketOnboardingError.AddressMismatch(expected = "0xA", actual = "0xB"),
            expected = resourceReference(R.string.prediction_onboarding_error_address_mismatch),
        ),
        FailureMessageModel(
            error = PolymarketOnboardingError.DeploymentFailed,
            expected = resourceReference(R.string.prediction_onboarding_error_deployment_failed),
        ),
        FailureMessageModel(
            error = PolymarketOnboardingError.ApprovalsFailed,
            expected = resourceReference(R.string.prediction_onboarding_error_approvals_failed),
        ),
        FailureMessageModel(
            error = PolymarketOnboardingError.Network,
            expected = resourceReference(R.string.common_unknown_error),
        ),
        FailureMessageModel(
            error = PolymarketOnboardingError.Unknown,
            expected = resourceReference(R.string.common_unknown_error),
        ),
    )

    private fun wallet(cause: PolymarketWalletError, expected: Int) = FailureMessageModel(
        error = PolymarketOnboardingError.Wallet(cause),
        expected = resourceReference(expected),
    )

    private fun signing(cause: PolymarketSigningError, expected: Int) = FailureMessageModel(
        error = PolymarketOnboardingError.Signing(cause),
        expected = resourceReference(expected),
    )
}