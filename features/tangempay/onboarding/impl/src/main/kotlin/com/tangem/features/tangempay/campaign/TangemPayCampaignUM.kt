package com.tangem.features.tangempay.campaign

import javax.annotation.concurrent.Immutable

@Immutable
internal sealed interface TangemPayCampaignUM {

    data object Loading : TangemPayCampaignUM

    data class Error(
        val onRetry: () -> Unit,
        val onClose: () -> Unit,
    ) : TangemPayCampaignUM

    data class Landing(
        val cardArtUrl: String?,
        val mode: Mode,
        val isCtaLoading: Boolean,
        val onGetCardClick: () -> Unit,
        val onTermsOfServiceClick: () -> Unit,
        val onPrivacyPolicyClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : TangemPayCampaignUM {

        enum class Mode { NoWallet, NoPay }
    }
}