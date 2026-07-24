package com.tangem.features.feed.ui.earn.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R

@Composable
internal inline fun <reified T : TangemBottomSheetConfigContent> EarnFilterBottomSheet(
    config: TangemBottomSheetConfig,
    crossinline content: @Composable (T) -> Unit,
) {
    TangemBottomSheet<T>(
        config = config,
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.primary,
        title = {
            TangemTopNavigation(
                title = resourceReference(R.string.earn_filter_by),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                blurBackground = false,
                onClose = config.onDismissRequest,
            )
        },
        content = content,
    )
}