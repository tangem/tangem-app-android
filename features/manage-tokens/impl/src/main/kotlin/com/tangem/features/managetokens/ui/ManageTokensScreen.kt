package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.managetokens.component.preview.PreviewManageTokensComponent
import com.tangem.feature.managetokens.entity.ManageTokensUM

@Composable
internal fun ManageTokensScreen(state: ManageTokensUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.popBack)

    Scaffold(
        modifier = modifier,
        content = {
            TODO("Not yet implemented")
        },
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ManageTokens() {
    TangemThemePreview {
        PreviewManageTokensComponent().Content(Modifier.fillMaxWidth())
    }
}
// endregion Preview