package com.tangem.features.collectibles.impl.packopenfailed.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun CollectiblesPackOpenFailedScreen(
    onGetAssistanceClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemStatusScreen(
        modifier = modifier,
        variant = TangemStatusScreen.Variant.Error,
        title = resourceReference(R.string.collectibles_open_pack_failed_title),
        description = resourceReference(R.string.collectibles_open_pack_failed_subtitle),
        asset = {
            TangemStatusIcon(kind = TangemStatusIcon.Kind.HeartBroken, modifier = Modifier.size(40.dp))
        },
        footer = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.collectibles_main_menu_get_assistance),
                    onClick = onGetAssistanceClick,
                )
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_close),
                    onClick = onCloseClick,
                )
            }
        },
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Preview(widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesPackOpenFailedScreen_Preview() {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = false) {
        CollectiblesPackOpenFailedScreen(onGetAssistanceClick = {}, onCloseClick = {})
    }
}