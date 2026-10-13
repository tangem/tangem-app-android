package com.tangem.features.collectibles.impl.packopening.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.packopening.ui.CollectiblesPackOpeningScreen
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesPackOpeningScreenPreview(
    @PreviewParameter(CollectiblesPackOpeningProvider::class) state: CollectiblesPackOpeningUM,
) {
    TangemThemePreviewRedesign {
        CollectiblesPackOpeningScreen(state = state)
    }
}

private class CollectiblesPackOpeningProvider : PreviewParameterProvider<CollectiblesPackOpeningUM> {
    override val values = sequenceOf(
        CollectiblesPackOpeningPreviewData.content,
        CollectiblesPackOpeningPreviewData.notEnoughFunds,
        CollectiblesPackOpeningPreviewData.loading,
        CollectiblesPackOpeningPreviewData.error,
    )
}