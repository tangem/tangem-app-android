package com.tangem.features.promobanners.impl.campaigns.model

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

internal data class CampaignContent(
    val name: String,
    val logo: TangemIconUM,
    val description: TextReference,
    val termsUrl: String,
    val learnMoreUrl: String,
)