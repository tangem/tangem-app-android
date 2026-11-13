package com.tangem.features.earn.impl.incentive.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.earn.impl.incentive.ui.state.EarnIncentiveSheetUM

@Composable
internal fun EarnIncentiveSheetContent(state: EarnIncentiveSheetUM, chart: @Composable () -> Unit) {
    TangemModal<EarnIncentiveSheetUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = state,
        ),
        title = { content -> Title(title = content.title) },
        content = { content ->
            Column {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { chart() }
                Footer(
                    title = content.footerTitle,
                    description = content.footerDescription,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp),
                )
            }
        },
    )
}

@Composable
private fun Title(title: TextReference, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .heightIn(min = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Footer(title: TextReference, description: TextReference, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = description.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Preview(name = "Light", widthDp = 360, heightDp = 640)
@Preview(name = "Dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnIncentiveSheetContentPreview() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(640.dp)
                .background(TangemTheme.colors3.bg.primary),
        ) {
            EarnIncentiveSheetContent(
                state = EarnIncentiveSheetUM(
                    title = resourceReference(R.string.yield_module_incentive_chart_sheet_title),
                    footerTitle = resourceReference(R.string.yield_module_incentive_chart_powered_by_title),
                    footerDescription = resourceReference(R.string.yield_module_incentive_chart_powered_by_description),
                    onDismiss = {},
                ),
                chart = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(TangemTheme.colors3.bg.secondary),
                    )
                },
            )
        }
    }
}