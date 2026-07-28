package com.tangem.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
fun UnableToLoadData(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    UnableToLoadDataV2(onRetryClick, modifier)
}

@Composable
private fun UnableToLoadDataV2(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.markets_loading_error_title),
            style = TangemTheme.typography2.bodyRegular14,
            color = TangemTheme.colors2.text.neutral.secondary,
        )
        TangemButton(
            buttonUM = TangemButtonUM(
                text = resourceReference(R.string.try_to_load_data_again_button_title),
                onClick = onRetryClick,
                type = TangemButtonType.Secondary,
                size = TangemButtonSize.X8,
                shape = TangemButtonShape.Rounded,
            ),
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun PreviewV2() {
    TangemThemePreviewRedesign {
        UnableToLoadDataV2(onRetryClick = {})
    }
}