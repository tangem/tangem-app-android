package com.tangem.feature.referral.models

internal data class ReferralStateHolder(
    val headerState: HeaderState,
    val referralInfoState: ReferralInfoState,
    val errorSnackbar: ErrorSnackbar? = null,
) {

    data class HeaderState(val onBackClicked: () -> Unit)

    sealed interface ReferralInfoContentState {
        val award: String
        val networkName: String
        val discount: String
        val url: String
    }

    sealed interface ReferralInfoState {
        data class ParticipantContent(
            override val award: String,
            override val networkName: String,
            val address: String,
            override val discount: String,
            val purchasedWalletCount: Int,
            val code: String,
            val shareLink: String,
            override val url: String,
        ) : ReferralInfoState, ReferralInfoContentState

        data class NonParticipantContent(
            override val award: String,
            override val networkName: String,
            override val discount: String,
            override val url: String,
            val onParticipateClicked: () -> Unit,
        ) : ReferralInfoState, ReferralInfoContentState

        object Loading : ReferralInfoState
    }

    data class ErrorSnackbar(
        val throwable: Throwable,
        val onOkClicked: () -> Unit,
    )
}

