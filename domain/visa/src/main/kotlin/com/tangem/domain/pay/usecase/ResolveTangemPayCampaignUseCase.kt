package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.error.UniversalError
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
 * 2. the campaign data (card art) is loaded;
 * 3. the wallets are checked for an existing Payment account. Without one the campaign resolves to
 *    [TangemPayCampaignResolution.Landing], with one the campaign offer is requested and resolves to
 *    [TangemPayCampaignResolution.IssueCard]; a response without the campaign offer means the campaign is
 *    over or the card limit is reached, so it resolves to [TangemPayCampaignResolution.Unavailable].
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

        val campaign = campaignRepository.getCampaign(campaignKey)
            .onLeft { error -> logFailure(step = "getCampaign", error = error) }
            .bind()

        val payWalletId = findWalletWithPay()
            .onLeft { error -> logFailure(step = "hasTangemPayInWallet", error = error) }
            .bind()
            ?: return@either TangemPayCampaignResolution.Landing(campaign)

        val offer = getCustomerOffersUseCase
            .campaignOffer(userWalletId = payWalletId, deeplinkValue = campaignKey)
            .onLeft { error -> logFailure(step = "campaignOffer", error = error) }
            .bind()

        if (offer == null) {
            TangemLogger.i("$TAG No campaign offer for key=$campaignKey")
            return@either TangemPayCampaignResolution.Unavailable
        }

        TangemPayCampaignResolution.IssueCard(userWalletId = payWalletId, campaign = campaign, offer = offer)
    }

    private suspend fun findWalletWithPay(): Either<VisaApiError, UserWalletId?> = either {
        eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = false)
            .firstOrNull { walletId -> onboardingRepository.hasTangemPayInWallet(walletId).bind() }
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