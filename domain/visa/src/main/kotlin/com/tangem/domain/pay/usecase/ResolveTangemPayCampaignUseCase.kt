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
import com.tangem.utils.logging.TangemLogger

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

        val issuableWalletId = findIssuableWallet() ?: return@either TangemPayCampaignResolution.Landing(campaign)

        val offer = getCustomerOffersUseCase
            .campaignOffer(userWalletId = issuableWalletId, deeplinkValue = campaignKey)
            .getOrNull()
            ?: return@either TangemPayCampaignResolution.Landing(campaign)

        TangemLogger.i("$TAG Opening the card order screen for $issuableWalletId right away")
        TangemPayCampaignResolution.IssueCard(userWalletId = issuableWalletId, campaign = campaign, offer = offer)
    }

    private suspend fun findIssuableWallet(): UserWalletId? {
        val walletId = eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = false)
            .singleOrNull()
            ?: return null

        if (onboardingRepository.hasTangemPayInWallet(walletId).getOrNull() != true) return null

        if (!onboardingRepository.isTangemPayInitialDataProduced(walletId)) {
            TangemLogger.i("$TAG Tangem Pay credentials of $walletId are missing, showing the landing")
            return null
        }

        val customerInfo = onboardingRepository.getCustomerInfo(walletId).getOrNull()
        if (customerInfo == null) {
            TangemLogger.i("$TAG Tangem Pay session of $walletId is not alive, showing the landing")
            return null
        }

        return walletId.takeIf { customerInfo.kycStatus == KycStatus.APPROVED }
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