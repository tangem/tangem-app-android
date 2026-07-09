package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.ui.NotActiveCampaignMessageContent

internal class NotActiveCampaignBottomSheetComponent(
    private val onDismissRequest: () -> Unit,
) : CampaignsModularComponent {

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        TangemTopNavigation(
            windowInsets = WindowInsets(0),
            blurBackground = false,
            endButton = { TangemButton.Close(onClick = onDismissRequest) },
        )
    }

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, contentPadding: PaddingValues, modifier: Modifier) {
        NotActiveCampaignMessageContent()
    }

    @Composable
    override fun Footer() {
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.common_close),
            onClick = { onDismissRequest() },
        )
    }
}