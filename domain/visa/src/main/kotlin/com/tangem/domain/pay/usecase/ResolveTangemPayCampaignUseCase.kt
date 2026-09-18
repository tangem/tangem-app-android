package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.model.TangemPayCampaignResolution
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCampaignRepository
import com.tangem.utils.logging.TangemLogger

class ResolveTangemPayCampaignUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val campaignRepository: TangemPayCampaignRepository,
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

        TangemPayCampaignResolution.Landing(campaign)
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