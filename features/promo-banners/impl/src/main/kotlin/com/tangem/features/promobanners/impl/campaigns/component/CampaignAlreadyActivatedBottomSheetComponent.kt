package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
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
import com.tangem.features.promobanners.impl.campaigns.ui.AlreadyActivatedCampaignContent

internal class CampaignAlreadyActivatedBottomSheetComponent(
    appComponentContext: AppComponentContext,
    params: Params,
    val onDismiss: () -> Unit,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val campaignName = CampaignTypeToContentConverter().convert(params.campaignType).name

    @Composable
    override fun Title() {
        TangemTopNavigation(
            windowInsets = WindowInsets(0),
            blurBackground = false,
            endButton = { TangemButton.Close(onClick = onDismiss) },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        AlreadyActivatedCampaignContent(
            message = resourceReference(
                R.string.promo_campaign_already_activated_title,
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
            onClick = onDismiss,
        )
    }

    data class Params(
        val campaignType: CampaignType,
    )
}