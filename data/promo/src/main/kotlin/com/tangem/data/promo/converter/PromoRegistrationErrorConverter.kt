package com.tangem.data.promo.converter

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.promo.models.PromoRegistrationError

/**
 * 400 and 404 each carry both errors with their own UI and generic ones, so the error body's message tells them
 * apart; 409 has a single meaning.
 */
internal object PromoRegistrationErrorConverter {

    private const val CAMPAIGN_NOT_FOUND = "campaign not found"
    private const val MAIN_WALLET_NOT_ELIGIBLE = "main wallet not eligible"

    fun convert(cause: Throwable, errorMessage: String?): PromoRegistrationError {
        val code = (cause as? ApiResponseError.HttpException)?.code
        val message = errorMessage?.trim()?.lowercase()
        return when {
            code == Code.NOT_FOUND && message == CAMPAIGN_NOT_FOUND -> PromoRegistrationError.CampaignNotFound
            code == Code.CONFLICT -> PromoRegistrationError.CampaignNotActive
            code == Code.BAD_REQUEST && message == MAIN_WALLET_NOT_ELIGIBLE ->
                PromoRegistrationError.MainWalletNotEligible
            else -> PromoRegistrationError.Generic(cause)
        }
    }
}