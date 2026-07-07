package com.tangem.features.promobanners.impl.campaigns.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.promobanners.impl.campaigns.entity.ActivateCampaignUM

@Composable
internal fun ActivateCampaignContent(
    state: ActivateCampaignUM,
    onSelectTokenClick: () -> Unit,
    onEnrollClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        onBack = onDismiss,
        title = {
            TangemTopNavigation(
                windowInsets = WindowInsets(0),
                blurBackground = false,
                endButton = {
                    TangemButton.Close(
                        onClick = onDismiss,
                    )
                },
            )
        },
        content = {
            ActivateCampaignContent(
                state = state,
                onSelectTokenClick = onSelectTokenClick,
                onEnrollClick = onEnrollClick,
                onLearnMoreClick = onLearnMoreClick,
            )
        },
    )
}