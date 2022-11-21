package com.tangem.feature.referral.models

data class ReferralStateHolder(
    val headerState: HeaderState,
    val referralInfoState: ReferralInfoState,
    val errorToast: ErrorToast,
) {

    data class HeaderState(val onBackClicked: () -> Unit)

    sealed interface ReferralInfoContentState {
        val award: String
        val discount: String
        val url: String
    }

    sealed interface ReferralInfoState {
        data class ParticipantContent(
            override val award: String,
            val address: String,
            override val discount: String,
            val purchasedWalletCount: Int,
            val code: String,
            override val url: String,
        ) : ReferralInfoState, ReferralInfoContentState

        data class NonParticipantContent(
            override val award: String,
            override val discount: String,
            override val url: String,
            val onParticipateClicked: () -> Unit,
        ) : ReferralInfoState, ReferralInfoContentState

        object Loading : ReferralInfoState
    }

    data class ErrorToast(
        val visibility: Boolean,
        val changeVisibility: () -> Unit
    )
}
