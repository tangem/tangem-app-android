package com.tangem.domain.promo.models

import com.tangem.domain.models.wallet.UserWalletId

data class PromoPayoutToken(
    val tokenId: String?,
    val tokenAddress: String,
    val tokenSymbol: String,
    val tokenName: String,
    val networkId: String,
    val decimals: Int?,
)

/**
 * One wallet to register for a campaign. [payout] is required when the campaign has [PromoPayout.isNeeded];
 * [batchId] only applies to [PromoWalletType.Card]. [isMain] marks the wallet that initiated the registration.
 */
data class PromoEnrollment(
    val userWalletId: UserWalletId,
    val walletType: PromoWalletType,
    val batchId: String?,
    val payout: PromoPayoutTarget?,
    val isMain: Boolean,
) {
    data class PromoPayoutTarget(
        val userAddress: String,
        val tokenAddress: String?,
        val networkId: String,
        val tokenId: String?,
    )
}

/** [payout] is `null` when the campaign needs no payout, even if the wallet is enrolled. */
data class PromoRegistrationResult(
    val userWalletId: UserWalletId,
    val status: PromoRegistrationStatus,
    val payout: PromoRegisteredPayout?,
) {
    enum class PromoRegistrationStatus {
        Enrolled,
        AlreadyEnrolled,
        Ineligible,
    }

    data class PromoRegisteredPayout(
        val userAddress: String,
        val tokenAddress: String,
        val networkId: String,
    )
}

/** Registration failures the UI shows on their own; every other failure is [Generic]. */
sealed interface PromoRegistrationError {

    data object CampaignNotFound : PromoRegistrationError

    /** The campaign hasn't started yet or is paused; registering may succeed later. */
    data object CampaignNotActive : PromoRegistrationError

    data object MainWalletNotEligible : PromoRegistrationError

    data class Generic(val cause: Throwable) : PromoRegistrationError
}