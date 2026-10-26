package com.tangem.features.promobanners.impl.campaigns.analytics

import arrow.core.Either
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.domain.promo.models.PromoRegistrationResult
import com.tangem.domain.promo.models.PromoRegistrationResult.PromoRegistrationStatus
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.EnrollError
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.EnrollSuccessful
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.PayoutToken
import com.tangem.features.promobanners.impl.campaigns.v2.model.PromoRegistrationOutcome

/** Follows the [PromoRegistrationOutcome] the user is shown, so the event always matches the screen. */
internal object PromoEnrollAnalyticsResolver {

    private val PromoRegistrationError.httpCode: Int?
        get() = when (this) {
            PromoRegistrationError.CampaignNotFound -> Code.NOT_FOUND.numericCode
            PromoRegistrationError.CampaignNotActive -> Code.CONFLICT.numericCode
            PromoRegistrationError.MainWalletNotEligible -> Code.BAD_REQUEST.numericCode
            is PromoRegistrationError.Generic -> (cause as? ApiResponseError.HttpException)?.code?.numericCode
        }

    fun resolve(
        campaign: PromoCampaign,
        payoutToken: PayoutToken?,
        result: Either<PromoRegistrationError, List<PromoRegistrationResult>>,
        outcome: PromoRegistrationOutcome,
        walletsRequested: Int,
    ): PromoCampaignsAnalyticsEvent {
        val errorCode = result.fold(ifLeft = { it.httpCode }, ifRight = { Code.OK.numericCode })
        return when (outcome) {
            is PromoRegistrationOutcome.Enrolled -> EnrollSuccessful(
                campaign = campaign,
                payoutToken = payoutToken,
                walletsEnrolled = result.getOrNull().orEmpty().count { it.status == PromoRegistrationStatus.Enrolled },
                walletsTotal = walletsRequested,
            )
            is PromoRegistrationOutcome.AlreadyEnrolled -> AlreadyEnrolledScreenOpened(
                campaign = campaign.name,
                source = AlreadyEnrolledScreenOpened.Source.EnrollResponse,
            )
            PromoRegistrationOutcome.Unavailable -> EnrollError(campaign, EnrollError.Reason.Unavailable, errorCode)
            PromoRegistrationOutcome.NotStarted -> EnrollError(campaign, EnrollError.Reason.NotActiveYet, errorCode)
            PromoRegistrationOutcome.Failed -> EnrollError(campaign, EnrollError.Reason.Technical, errorCode)
        }
    }
}