package com.tangem.feature.referral.models

data class ReferralStateHolder(
    val headerState: HeaderState,
    val referralInfoState: ReferralInfoState,
    val effects: Effects,
) {

    data class HeaderState(val onBackClicked: () -> Unit)

    sealed interface ReferralInfoContentState

    sealed interface ReferralInfoState {

        data class ParticipantContent(
            val award: String,
            val address: String,
            val discount: String,
            val onAgreementClicked: () -> Unit,
            val onParticipateClicked: () -> Unit,
        ) : ReferralInfoState, ReferralInfoContentState

        data class NonParticipantContent(
            val award: String,
            val discount: String,
            val purchasedWalletCount: Int,
            val code: String,
            val onCopyClicked: () -> Unit,
            val onShareClicked: () -> Unit,
            val onAgreementClicked: () -> Unit,
        ) : ReferralInfoState, ReferralInfoContentState

        object Loading : ReferralInfoState
    }

    data class Effects(val showErrorToast: Boolean)
}
