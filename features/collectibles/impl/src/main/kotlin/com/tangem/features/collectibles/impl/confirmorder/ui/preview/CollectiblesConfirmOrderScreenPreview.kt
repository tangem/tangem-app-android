package com.tangem.features.collectibles.impl.confirmorder.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.confirmorder.ui.CollectiblesConfirmOrderScreen
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesConfirmOrderScreenPreview(
    @PreviewParameter(CollectiblesConfirmOrderProvider::class) state: CollectiblesConfirmOrderUM,
) {
    TangemThemePreviewRedesign {
        CollectiblesConfirmOrderScreen(state = state)
    }
}

private class CollectiblesConfirmOrderProvider : PreviewParameterProvider<CollectiblesConfirmOrderUM> {
    override val values = sequenceOf(
        CollectiblesConfirmOrderPreviewData.content,
        CollectiblesConfirmOrderPreviewData.notEnoughFunds,
        CollectiblesConfirmOrderPreviewData.loading,
        CollectiblesConfirmOrderPreviewData.error,
    )
}