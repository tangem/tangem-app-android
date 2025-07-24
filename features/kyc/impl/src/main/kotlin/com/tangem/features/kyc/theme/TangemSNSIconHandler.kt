package com.tangem.features.kyc.theme

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.sumsub.sns.core.data.listener.SNSDefaultIconHandler
import com.sumsub.sns.core.data.listener.SNSIconHandler
import com.tangem.features.kyc.impl.R

/**
 * Custom icons' settings for SumSub SDK
 */
internal class TangemSNSIconHandler : SNSDefaultIconHandler() {
    @Suppress("CyclomaticComplexMethod")
    override fun onResolveIcon(context: Context, key: String): Drawable? {
        val iconRes = when {
            key == "IdentityType/PASSPORT" -> R.drawable.ic_kyc_passport_22
            key == "IdentityType/DRIVERS" -> R.drawable.ic_kyc_drivers_24
            key == SNSIconHandler.SNSCommonIcons.COUNTRY_OTHER.imageName -> R.drawable.ic_kyc_other_countries_24
            key.startsWith("IdentityType/") -> R.drawable.ic_kyc_drivers_24
            key == "DocType/IDENTITY" -> R.drawable.ic_kyc_identity_22
            key == "DocType/PROOF_OF_RESIDENCE" -> R.drawable.ic_kyc_proof_of_residence_22
            key == "DocType/SELFIE" -> R.drawable.ic_kyc_selfie_22
            key == "DocType/APPLICANT_DATA" -> R.drawable.ic_kyc_profile_data_22
            key == "DocType/QUESTIONNAIRE" -> R.drawable.ic_kyc_questionnaire_22
            key == "DocType/PHONE_VERIFICATION" -> R.drawable.ic_kyc_phone_verification_24
            key == "DocType/EMAIL_VERIFICATION" -> R.drawable.ic_kyc_email_verification_22
            key == SNSIconHandler.SNSCommonIcons.PICTURE_AGREEMENT.imageName -> -1
            else -> null
        }
        return iconRes?.let {
            if (iconRes != -1) ResourcesCompat.getDrawable(context.resources, it, context.theme) else null
        } ?: run { super.onResolveIcon(context, key) }
    }
}