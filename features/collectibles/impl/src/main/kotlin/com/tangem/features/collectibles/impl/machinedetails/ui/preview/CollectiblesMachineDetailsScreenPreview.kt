package com.tangem.features.collectibles.impl.machinedetails.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.machinedetails.ui.CollectiblesMachineDetailsScreen
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM

@Preview(showBackground = true, widthDp = 402, heightDp = 1874)
@Preview(showBackground = true, widthDp = 402, heightDp = 1874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesMachineDetailsScreenPreview(
    @PreviewParameter(CollectiblesPackDetailsProvider::class) state: CollectiblesMachineDetailsUM,
) {
    TangemThemePreviewRedesign {
        CollectiblesMachineDetailsScreen(state = state, onLoadMore = {})
    }
}

private class CollectiblesPackDetailsProvider : PreviewParameterProvider<CollectiblesMachineDetailsUM> {

    override val values = sequenceOf(
        CollectiblesMachineDetailsPreviewData.content,
        CollectiblesMachineDetailsPreviewData.withoutEpicTier,
        CollectiblesMachineDetailsPreviewData.poolLoading,
        CollectiblesMachineDetailsPreviewData.poolError,
        CollectiblesMachineDetailsPreviewData.poolEmpty,
        CollectiblesMachineDetailsPreviewData.loading,
        CollectiblesMachineDetailsPreviewData.error,
    )
}