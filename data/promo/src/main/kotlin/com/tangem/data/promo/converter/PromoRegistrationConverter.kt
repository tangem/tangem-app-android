package com.tangem.data.promo.converter

import com.tangem.datasource.api.promotion.models.PromotionRegistrationsBody.EnrollmentDto
import com.tangem.datasource.api.promotion.models.PromotionRegistrationsResponse.ResultDto
import com.tangem.datasource.api.promotion.models.PromotionRegistrationsResponse.Status
import com.tangem.datasource.api.promotion.models.PromotionWalletType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.*

internal object PromoRegistrationConverter {

    fun toDto(enrollment: PromoEnrollment) = EnrollmentDto(
        walletId = enrollment.userWalletId.stringValue,
        walletType = when (enrollment.walletType) {
            PromoWalletType.Card -> PromotionWalletType.CARD
            PromoWalletType.Mobile -> PromotionWalletType.MOBILE
        },
        batchId = enrollment.batchId.takeIf { enrollment.walletType == PromoWalletType.Card },
        userAddress = enrollment.payout?.userAddress,
        tokenAddress = enrollment.payout?.tokenAddress,
        networkId = enrollment.payout?.networkId,
        tokenId = enrollment.payout?.tokenId,
        isMain = enrollment.isMain,
    )

    /**
     * A status this app version doesn't know can't be shown as either outcome, and a wallet id that isn't a
     * [UserWalletId] can't be matched to a wallet, so such a result is dropped.
     */
    fun toDomain(result: ResultDto): PromoRegistrationResult? {
        return PromoRegistrationResult(
            userWalletId = runCatching { UserWalletId(result.walletId) }.getOrNull() ?: return null,
            status = when (result.status) {
                Status.ENROLLED -> PromoRegistrationResult.PromoRegistrationStatus.Enrolled
                Status.ALREADY_ENROLLED -> PromoRegistrationResult.PromoRegistrationStatus.AlreadyEnrolled
                Status.INELIGIBLE -> PromoRegistrationResult.PromoRegistrationStatus.Ineligible
                Status.UNKNOWN -> return null
            },
            payout = result.payout?.let { payout ->
                PromoRegistrationResult.PromoRegisteredPayout(
                    userAddress = payout.userAddress,
                    tokenAddress = payout.tokenAddress,
                    networkId = payout.networkId,
                )
            },
        )
    }
}