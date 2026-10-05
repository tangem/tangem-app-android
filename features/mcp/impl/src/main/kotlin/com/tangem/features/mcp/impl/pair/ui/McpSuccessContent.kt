package com.tangem.features.mcp.impl.pair.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_28
import com.tangem.features.mcp.impl.R
import com.tangem.features.mcp.impl.pair.model.McpPairUM

@Composable
internal fun McpSuccessContent(state: McpPairUM.Success, modifier: Modifier = Modifier) {
    TangemModalStatus(
        modifier = modifier,
        variant = TangemModalStatus.Variant.Info,
        asset = {
            TangemModalStatus.Avatar(
                variant = TangemModalStatus.Variant.Info,
                imageVector = Icons.ic_checkmark_28,
            )
        },
        title = resourceReference(R.string.mcp_connection_success_title),
        description = resourceReference(
            id = R.string.mcp_connection_success_description,
            formatArgs = wrappedList(state.agentName),
        ),
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun McpSuccessContentPreview() {
    TangemThemePreviewRedesign {
        McpSuccessContent(
            state = McpPairUM.Success(agentName = stringReference("Claude"), onDone = {}),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}