package com.tangem.features.tangempay.common.kyc

import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference

fun createKycAdditionalInfoBannerState(onProvideInformationClick: () -> Unit): TangemMessageBanner.State =
    TangemMessageBanner.State(
        title = resourceReference(R.string.tangempay_kyc_additional_info_title),
        variant = TangemMessageBanner.Variant.Warning,
        description = resourceReference(R.string.tangempay_kyc_additional_info_banner_description),
        secondaryButton = TangemMessageBanner.Button(
            text = resourceReference(R.string.tangempay_kyc_additional_info_button),
            onClick = onProvideInformationClick,
        ),
    )