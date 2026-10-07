package com.tangem.features.promobanners.impl.campaigns.v2.model

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoEnrollment
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.domain.promo.models.PromoRegistrationResult
import com.tangem.domain.promo.models.PromoRegistrationResult.PromoRegistrationStatus

internal sealed interface PromoRegistrationOutcome {

    /** [enrolledWalletIds] includes wallets enrolled before this request. */
    data class Enrolled(val coverage: Coverage, val enrolledWalletIds: List<UserWalletId>) : PromoRegistrationOutcome

    data class AlreadyEnrolled(val payout: PromoRegistrationResult.PromoRegisteredPayout?) : PromoRegistrationOutcome

    data object Unavailable : PromoRegistrationOutcome

    data object NotStarted : PromoRegistrationOutcome

    data object Failed : PromoRegistrationOutcome

    enum class Coverage {
        Single,
        All,
        Partial,
    }

    companion object {

        /** The main wallet decides the outcome; a wallet missing from the results counts as not enrolled. */
        fun resolve(
            mainWalletId: UserWalletId,
            enrollments: List<PromoEnrollment>,
            result: Either<PromoRegistrationError, List<PromoRegistrationResult>>,
        ): PromoRegistrationOutcome = result.fold(
            ifLeft = ::fromError,
            ifRight = { results -> fromResults(mainWalletId, enrollments, results) },
        )

        private fun fromError(error: PromoRegistrationError): PromoRegistrationOutcome = when (error) {
            PromoRegistrationError.CampaignNotFound,
            PromoRegistrationError.MainWalletNotEligible,
            -> Unavailable
            PromoRegistrationError.CampaignNotActive -> NotStarted
            is PromoRegistrationError.Generic -> Failed
        }

        private fun fromResults(
            mainWalletId: UserWalletId,
            enrollments: List<PromoEnrollment>,
            results: List<PromoRegistrationResult>,
        ): PromoRegistrationOutcome {
            val resultsByWallet = results.associateBy(PromoRegistrationResult::userWalletId)
            val main = resultsByWallet[mainWalletId]
            if (main == null || main.status == PromoRegistrationStatus.Ineligible) return Unavailable

            if (results.none { it.status == PromoRegistrationStatus.Enrolled }) {
                return AlreadyEnrolled(payout = main.payout)
            }

            val enrolledWalletIds = enrollments
                .map(PromoEnrollment::userWalletId)
                .filter { walletId -> resultsByWallet[walletId]?.status?.isInCampaign() == true }
            val coverage = when {
                enrollments.size == 1 -> Coverage.Single
                enrolledWalletIds.size == enrollments.size -> Coverage.All
                else -> Coverage.Partial
            }

            return Enrolled(coverage = coverage, enrolledWalletIds = enrolledWalletIds)
        }

        private fun PromoRegistrationStatus.isInCampaign(): Boolean = when (this) {
            PromoRegistrationStatus.Enrolled,
            PromoRegistrationStatus.AlreadyEnrolled,
            -> true
            PromoRegistrationStatus.Ineligible -> false
        }
    }
}