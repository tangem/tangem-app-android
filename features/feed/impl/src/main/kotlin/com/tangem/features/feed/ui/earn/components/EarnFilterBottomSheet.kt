package com.tangem.features.feed.ui.earn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
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
        containerColor = TangemTheme.colors2.surface.level3,
        title = {
            TangemTopBar(
                title = resourceReference(R.string.earn_filter_by),
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_close_24),
                        contentDescription = null,
                        tint = TangemTheme.colors2.graphic.neutral.primary,
                        modifier = Modifier
                            .size(TangemTheme.dimens2.x11)
                            .background(
                                color = TangemTheme.colors2.button.backgroundSecondary,
                                shape = CircleShape,
                            )
                            .clickableSingle(onClick = config.onDismissRequest)
                            .padding(TangemTheme.dimens2.x2_5),
                    )
                },
            )
        },
        content = content,
    )
}