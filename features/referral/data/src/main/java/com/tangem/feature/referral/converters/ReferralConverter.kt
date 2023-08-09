package com.tangem.feature.referral.converters

import com.tangem.datasource.api.tangemTech.models.ReferralResponse
import com.tangem.feature.referral.domain.models.*
import com.tangem.utils.converter.Converter
import com.tangem.utils.safeValueOf
import org.joda.time.format.ISODateTimeFormat
import javax.inject.Inject

class ReferralConverter @Inject constructor() : Converter<ReferralResponse, ReferralData> {

    override fun convert(value: ReferralResponse): ReferralData {
        val conditions = value.conditions
        val referral = value.referral
        val tokenConverter = TokenConverter()
        val expectedAwardsConverter = ExpectedAwardsConverter()
        return if (referral != null) {
            ReferralData.ParticipantData(
                award = conditions.awards.firstOrNull()?.amount ?: 0,
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
                expectedAwards = value.expectedAwards?.let {
                    expectedAwardsConverter.convert(it)
                }
            )
        } else {
            ReferralData.NonParticipantData(
                award = conditions.awards.firstOrNull()?.amount ?: 0,
                discount = conditions.discount.amount,
                discountType = safeValueOf(conditions.discount.discountType.uppercase(), DiscountType.PERCENTAGE),
                tosLink = conditions.tosLink,
                tokens = tokenConverter.convertList(conditions.awards.map { it.token }),
            )
        }
    }
}

private class ExpectedAwardsConverter : Converter<ReferralResponse.ExpectedAwards, ExpectedAwards> {

    override fun convert(value: ReferralResponse.ExpectedAwards): ExpectedAwards {
        return ExpectedAwards(
            numberOfWallets = value.numberOfWallets,
            expectedAwards = value.list.map {
                ExpectedAward(
                    paymentDate = it.paymentDate.toString(),
                    amount = "${it.amount} ${it.currency}"
                )
            }
        )
    }
}

private class TokenConverter : Converter<ReferralResponse.Conditions.Award.Token, TokenData> {

    override fun convert(value: ReferralResponse.Conditions.Award.Token): TokenData {
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
