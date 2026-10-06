package com.tangem.data.promo.converter

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.promo.models.PromoRegistrationError

/** Only the HTTP code decides the error; a 400 is always [PromoRegistrationError.Generic], whatever its body says. */
internal object PromoRegistrationErrorConverter {

    fun convert(cause: Throwable): PromoRegistrationError {
        val code = (cause as? ApiResponseError.HttpException)?.code
        return when (code) {
            Code.NOT_FOUND -> PromoRegistrationError.CampaignNotFound
            Code.CONFLICT -> PromoRegistrationError.CampaignNotActive
            else -> PromoRegistrationError.Generic(cause)
        }
    }
}