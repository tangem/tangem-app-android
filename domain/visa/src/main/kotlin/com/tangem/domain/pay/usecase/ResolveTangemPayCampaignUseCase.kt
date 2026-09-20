package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.TangemPayCampaignResolution
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCampaignRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.logging.TangemLogger

/**
 * Resolves a co-brand campaign before the container can show anything:
 *
 * 1. the campaign link is validated by the backend — a rejected campaign resolves to
 *    [TangemPayCampaignResolution.Unavailable];
 * 2. the wallets are checked for an existing Payment account. Without one the campaign resolves to
 *    [TangemPayCampaignResolution.Landing], with one the campaign offer is requested and resolves to
 *    [TangemPayCampaignResolution.IssueCard]; a response without the campaign offer means the campaign is
 *    over or the card limit is reached, so it resolves to [TangemPayCampaignResolution.Unavailable];
 * 3. the campaign card art is loaded last, once the campaign is known to be issuable — a campaign
 *    without art still resolves, the card area shows its own placeholder.
 *    Only an account whose KYC is approved can issue a card, so a wallet still under review is skipped and,
 *    when no wallet qualifies, the campaign resolves to [TangemPayCampaignResolution.Landing]: its CTA
 *    attaches the campaign order and returns the user to the pending verification, the same way the regular
 *    onboarding does from the main-screen banner.
 *
 * Step 2 is unanswerable for a wallet whose Tangem Pay session is gone: whether it holds a Payment account is
 * known from the static token, but the account state behind the user token is not. That is recoverable rather
 * than fatal, so it resolves to [TangemPayCampaignResolution.SessionExpired] and the caller renews the session
 * before resolving again. A wallet that resolves outright wins over one that would need the renewal.
 *
 * Every request failure propagates as [Either.Left] — the caller shows a retryable error for it.
 */
class ResolveTangemPayCampaignUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val campaignRepository: TangemPayCampaignRepository,
    private val eligibilityManager: TangemPayEligibilityManager,
    private val getCustomerOffersUseCase: GetCustomerOffersUseCase,
) {

    suspend operator fun invoke(campaignKey: String): Either<UniversalError, TangemPayCampaignResolution> = either {
        val isValid = onboardingRepository.validateDeeplink(campaignLink(campaignKey))
            .onLeft { error -> logFailure(step = "validateDeeplink", error = error) }
            .bind()
        if (!isValid) {
            TangemLogger.i("$TAG Campaign is not valid: $campaignKey")
            return@either TangemPayCampaignResolution.Unavailable
        }

        val campaignWallet = findWalletForCampaign()
            .onLeft { error -> logFailure(step = "findWalletForCampaign", error = error) }
            .bind()

        val payWalletId = when (campaignWallet) {
            is CampaignWallet.Issuable -> campaignWallet.userWalletId
            is CampaignWallet.SessionExpired -> {
                return@either TangemPayCampaignResolution.SessionExpired(campaignWallet.userWalletId)
            }
            CampaignWallet.None -> null
        }

        val offer = if (payWalletId == null) {
            null
        } else {
            getCustomerOffersUseCase
                .campaignOffer(userWalletId = payWalletId, deeplinkValue = campaignKey)
                .onLeft { error -> logFailure(step = "campaignOffer", error = error) }
                .bind()
                ?: run {
                    TangemLogger.i("$TAG No campaign offer for key=$campaignKey")
                    return@either TangemPayCampaignResolution.Unavailable
                }
        }

        val campaign = campaignRepository.getCampaign(campaignKey)
            .onLeft { error -> logFailure(step = "getCampaign", error = error) }
            .bind()

        if (payWalletId == null || offer == null) {
            return@either TangemPayCampaignResolution.Landing(campaign)
        }

        TangemPayCampaignResolution.IssueCard(userWalletId = payWalletId, campaign = campaign, offer = offer)
    }

    private suspend fun findWalletForCampaign(): Either<VisaApiError, CampaignWallet> = either {
        var walletWithExpiredSession: CampaignWallet.SessionExpired? = null

        for (walletId in eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = false)) {
            when (val state = walletState(walletId).bind()) {
                is CampaignWallet.Issuable -> return@either state
                is CampaignWallet.SessionExpired -> walletWithExpiredSession = walletWithExpiredSession ?: state
                CampaignWallet.None -> Unit
            }
        }

        walletWithExpiredSession ?: CampaignWallet.None
    }

    private suspend fun walletState(walletId: UserWalletId): Either<VisaApiError, CampaignWallet> = either {
        if (!onboardingRepository.hasTangemPayInWallet(walletId).bind()) return@either CampaignWallet.None

        if (!onboardingRepository.isTangemPayInitialDataProduced(walletId)) {
            TangemLogger.i("$TAG Tangem Pay credentials of $walletId are missing, renewal is required")
            return@either CampaignWallet.SessionExpired(walletId)
        }

        onboardingRepository.getCustomerInfo(walletId).fold(
            ifLeft = { error ->
                if (error != VisaApiError.RefreshTokenExpired) raise(error)
                TangemLogger.i("$TAG Tangem Pay session of $walletId expired, renewal is required")
                CampaignWallet.SessionExpired(walletId)
            },
            ifRight = { customerInfo ->
                val kycStatus = customerInfo.kycStatus
                if (kycStatus == KycStatus.APPROVED) {
                    CampaignWallet.Issuable(walletId)
                } else {
                    TangemLogger.i("$TAG Payment account of $walletId is still under review (kyc=$kycStatus)")
                    CampaignWallet.None
                }
            },
        )
    }

    private fun campaignLink(campaignKey: String): String = "$MARKETING_LINK_PREFIX$campaignKey"

    private fun logFailure(step: String, error: UniversalError) {
        TangemLogger.e("$TAG Failed to resolve campaign at $step: $error")
    }

    private sealed interface CampaignWallet {

        data class Issuable(val userWalletId: UserWalletId) : CampaignWallet

        data class SessionExpired(val userWalletId: UserWalletId) : CampaignWallet

        data object None : CampaignWallet
    }

    private companion object {
        const val TAG = "[TangemPay][Campaign]"
        const val MARKETING_LINK_PREFIX = "tangem://marketing/"
    }
}