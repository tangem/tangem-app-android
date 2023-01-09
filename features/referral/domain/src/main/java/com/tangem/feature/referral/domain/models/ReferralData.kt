package com.tangem.feature.referral.domain.models

import org.joda.time.DateTime

sealed interface ReferralData {

    val award: Int
    val discount: Int
    val discountType: DiscountType
    val tosLink: String
    val tokens: List<TokenData>

    /** Data class that used if user is participant of program */
    data class ParticipantData(
        override val award: Int,
        override val discount: Int,
        override val discountType: DiscountType,
        override val tosLink: String,
        override val tokens: List<TokenData>,
        val referral: ReferralInfo,
    ) : ReferralData

    /** Data class that used if user is not participant of program */
    data class NonParticipantData(
        override val award: Int,
        override val discount: Int,
        override val discountType: DiscountType,
        override val tosLink: String,
        override val tokens: List<TokenData>,
    ) : ReferralData
}

data class TokenData(
    val id: String,
    val name: String,
    val symbol: String,
    val networkId: String,
    val contractAddress: String?,
    val decimalCount: Int?,
)

data class ReferralInfo(
    val shareLink: String,
    val address: String,
    val promocode: String,
    val walletsPurchased: Int,
    val termsAcceptedAt: DateTime?,
)

enum class DiscountType {
    PERCENTAGE, VALUE
}
