package com.tangem.core.ui.components.transactions

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
fun TxHistoryDateHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = TangemTheme.colors2.text.neutral.primary,
        style = TangemTheme.typography2.bodyMedium16,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = TangemTheme.dimens2.x4,
                end = TangemTheme.dimens2.x4,
                top = TangemTheme.dimens2.x6,
                bottom = TangemTheme.dimens2.x3,
            ),
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TxHistoryDateHeader() {
    TangemThemePreviewRedesign {
        TxHistoryDateHeader(title = "Today")
    }
}