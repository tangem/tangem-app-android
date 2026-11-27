package com.tangem.features.polymarket.impl.onboarding.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PolymarketWalletError

internal fun PolymarketOnboardingError.toFailureMessage(): TextReference? = when (this) {
    is PolymarketOnboardingError.Wallet -> cause.toFailureMessage()
    is PolymarketOnboardingError.Signing -> cause.toFailureMessage()
    is PolymarketOnboardingError.Auth -> resourceReference(R.string.prediction_onboarding_error_credentials)
    is PolymarketOnboardingError.AddressMismatch ->
        resourceReference(R.string.prediction_onboarding_error_address_mismatch)
    PolymarketOnboardingError.DeploymentFailed ->
        resourceReference(R.string.prediction_onboarding_error_deployment_failed)
    PolymarketOnboardingError.ApprovalsFailed ->
        resourceReference(R.string.prediction_onboarding_error_approvals_failed)
    PolymarketOnboardingError.Network,
    PolymarketOnboardingError.Unknown,
    -> unknownFailure()
}

private fun PolymarketWalletError.toFailureMessage(): TextReference = when (this) {
    PolymarketWalletError.InvalidRequest,
    PolymarketWalletError.Unauthorized,
    -> resourceReference(R.string.prediction_onboarding_error_request_declined)
    is PolymarketWalletError.RelayerRejected ->
        resourceReference(R.string.prediction_onboarding_error_transaction_rejected)
    PolymarketWalletError.RelayerUnavailable ->
        resourceReference(R.string.prediction_onboarding_error_polymarket_unavailable)
    PolymarketWalletError.WalletNotDeployed,
    PolymarketWalletError.Network,
    is PolymarketWalletError.Unexpected,
    -> unknownFailure()
}

private fun PolymarketSigningError.toFailureMessage(): TextReference? = when (this) {
    PolymarketSigningError.UserCancelled -> null
    PolymarketSigningError.NotDerived,
    PolymarketSigningError.MissingWallet,
    PolymarketSigningError.DerivationUnsupported,
    -> resourceReference(R.string.prediction_onboarding_error_wallet_unsupported)
    PolymarketSigningError.CardError -> resourceReference(R.string.prediction_onboarding_error_card_failed)
    PolymarketSigningError.Unknown,
    is PolymarketSigningError.SessionKey,
    -> unknownFailure()
}

private fun unknownFailure(): TextReference = resourceReference(R.string.common_unknown_error)