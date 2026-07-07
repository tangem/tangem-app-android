package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.model.CampaignAlreadyActivatedModel
import com.tangem.features.promobanners.impl.campaigns.ui.ActivateCampaignContent

internal class CampaignAlreadyActivatedBottomSheetComponent(
    appComponentContext: AppComponentContext,
    private val params: CampaignAlreadyActivatedModel.Params,
) : CampaignsModularComponent, AppComponentContext by appComponentContext {

    private val model: CampaignAlreadyActivatedModel = getOrCreateModel(params)

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        TangemTopNavigation(
            windowInsets = WindowInsets(0),
            blurBackground = false,
            endButton = { TangemButton.Close(onClick = params.onDismiss) },
        )
    }

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, contentPadding: PaddingValues, modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        ActivateCampaignContent(um = state)
    }

    @Composable
    override fun Footer() {
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.common_close),
            onClick = params.onDismiss,
        )
    }
}