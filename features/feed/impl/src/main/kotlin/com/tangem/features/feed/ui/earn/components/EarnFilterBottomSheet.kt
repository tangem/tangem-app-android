package com.tangem.features.feed.ui.earn.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R

@Composable
internal inline fun <reified T : TangemBottomSheetConfigContent> EarnFilterBottomSheet(
    config: TangemBottomSheetConfig,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    TangemBottomSheet<T>(
        config = config,
        containerColor = TangemTheme.colors2.surface.level3,
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TangemTheme.dimens.spacing24)
                    .padding(bottom = TangemTheme.dimens.spacing12),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResourceSafe(R.string.earn_filter_by),
                    style = TangemTheme.typography2.headingSemibold17,
                    color = TangemTheme.colors2.text.neutral.primary,
                    textAlign = TextAlign.Center,
                )
                SecondaryTangemButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = config.onDismissRequest,
                    iconRes = R.drawable.ic_close_24,
                    size = TangemButtonSize.X9,
                    shape = TangemButtonShape.Rounded,
                )
            }
        },
        content = content,
    )
}