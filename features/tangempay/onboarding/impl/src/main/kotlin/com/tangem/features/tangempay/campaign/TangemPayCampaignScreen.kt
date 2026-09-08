package com.tangem.features.tangempay.campaign

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun TangemPayCampaignScreen(state: TangemPayCampaignUM, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayCampaignUM.Loading -> TangemPayCampaignLoading(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding(),
        )
    }
}

@Composable
private fun TangemPayCampaignLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TangemTheme.colors.icon.primary1)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCampaignScreenPreview() {
    TangemThemePreviewRedesign {
        TangemPayCampaignScreen(state = TangemPayCampaignUM.Loading)
    }
}