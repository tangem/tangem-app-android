package com.tangem.feature.referral.converters

import com.tangem.datasource.api.referral.models.ReferralResponse
import com.tangem.datasource.api.referral.models.Token
import com.tangem.feature.referral.domain.models.DiscountType
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.ReferralInfo
import com.tangem.feature.referral.domain.models.TokenData
import com.tangem.utils.converter.Converter
import com.tangem.utils.safeValueOf
import org.joda.time.format.ISODateTimeFormat
import javax.inject.Inject

class ReferralConverter @Inject constructor() : Converter<ReferralResponse, ReferralData> {

    override fun convert(value: ReferralResponse): ReferralData {
        val conditions = value.conditions
        val referral = value.referral
        val tokenConverter = TokenConverter()
        return if (referral != null) {
            ReferralData.ParticipantData(
                award = conditions.award,
                discount = conditions.discount.amount,
                discountType = safeValueOf(conditions.discount.discountType.uppercase(), DiscountType.PERCENTAGE),
                tosLink = conditions.tosLink,
                tokens = tokenConverter.convertList(conditions.awards.map { it.token }),
                referral = ReferralInfo(
                    shareLink = referral.shareLink,
                    address = referral.address,
                    promocode = referral.promocode,
                    walletsPurchased = referral.walletsPurchased,
                    termsAcceptedAt = referral.termsAcceptedAt?.let {
                        ISODateTimeFormat.dateTimeParser().parseDateTime(referral.termsAcceptedAt)
                    },
                ),
            )
        } else {
            ReferralData.NonParticipantData(
                award = conditions.award,
                discount = conditions.discount.amount,
                discountType = safeValueOf(conditions.discount.discountType.uppercase(), DiscountType.PERCENTAGE),
                tosLink = conditions.tosLink,
                tokens = tokenConverter.convertList(conditions.awards.map { it.token }),
            )
        }
    }
}

private class TokenConverter : Converter<Token, TokenData> {

    override fun convert(value: Token): TokenData {
        return TokenData(
            id = value.id,
            name = value.name,
            symbol = value.symbol,
            networkId = value.networkId,
            contractAddress = value.contractAddress,
            decimalCount = value.decimalCount,
        )
    }
}
