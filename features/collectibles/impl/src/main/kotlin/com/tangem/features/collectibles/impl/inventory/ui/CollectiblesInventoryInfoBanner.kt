package com.tangem.features.collectibles.impl.inventory.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.features.collectibles.impl.inventory.ui.preview.CollectiblesInventoryPreviewData
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM.ItemUM

@Composable
internal fun CollectiblesInventoryInfoBanner(state: ItemUM.Info, modifier: Modifier = Modifier) {
    TangemMessageBanner(
        title = state.text,
        modifier = modifier.fillMaxWidth(),
        slotEnd = {
            TangemIcon(
                TangemIconUM.Icon(
                    imageVector = Icons.ic_info_20,
                    tintReference = { TangemTheme.colors3.icon.primary },
                ),
            )
        },
    )
}

@Preview(widthDp = 402, showBackground = true)
@Preview(widthDp = 402, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesInventoryInfoBannerPreview() {
    TangemThemePreviewRedesign {
        CollectiblesInventoryInfoBanner(
            state = CollectiblesInventoryPreviewData.info,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}