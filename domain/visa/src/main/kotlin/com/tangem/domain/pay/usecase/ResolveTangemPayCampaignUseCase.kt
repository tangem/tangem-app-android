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

        val payWalletId = findWalletWithIssuableAccount()
            .onLeft { error -> logFailure(step = "findWalletWithIssuableAccount", error = error) }
            .bind()

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

    private suspend fun findWalletWithIssuableAccount(): Either<VisaApiError, UserWalletId?> = either {
        eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = false)
            .firstOrNull { walletId -> canIssueCard(walletId).bind() }
    }

    private suspend fun canIssueCard(walletId: UserWalletId): Either<VisaApiError, Boolean> = either {
        if (!onboardingRepository.hasTangemPayInWallet(walletId).bind()) return@either false

        val kycStatus = onboardingRepository.getCustomerInfo(walletId).bind().kycStatus
        if (kycStatus != KycStatus.APPROVED) {
            TangemLogger.i("$TAG Payment account of $walletId is still under review (kyc=$kycStatus)")
        }
        kycStatus == KycStatus.APPROVED
    }

    private fun campaignLink(campaignKey: String): String = "$MARKETING_LINK_PREFIX$campaignKey"

    private fun logFailure(step: String, error: UniversalError) {
        TangemLogger.e("$TAG Failed to resolve campaign at $step: $error")
    }

    private companion object {
        const val TAG = "[TangemPay][Campaign]"
        const val MARKETING_LINK_PREFIX = "tangem://marketing/"
    }
}