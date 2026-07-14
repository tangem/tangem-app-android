package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignTypeToContentConverter
import com.tangem.features.promobanners.impl.campaigns.ui.CampaignEnrolledMessageContent

internal class CampaignEnrolledBottomSheetComponent(
    params: Params,
    private val onDismissRequest: () -> Unit,
) : ComposableModularContentComponent {

    private val campaignName = CampaignTypeToContentConverter().convert(params.campaignType).name

    @Composable
    override fun Title() {
        TangemTopNavigation(
            windowInsets = WindowInsets(0),
            blurBackground = false,
            endButton = { TangemButton.Close(onClick = onDismissRequest) },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        CampaignEnrolledMessageContent(
            message = resourceReference(
                R.string.promo_campaign_enroll_success_title,
                wrappedList(campaignName),
            ),
            modifier = modifier,
        )
    }

    @Composable
    override fun Footer() {
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.common_close),
            onClick = { onDismissRequest() },
        )
    }

    data class Params(
        val campaignType: CampaignType,
    )
}