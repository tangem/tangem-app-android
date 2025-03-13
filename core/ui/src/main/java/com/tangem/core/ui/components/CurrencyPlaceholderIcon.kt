package com.tangem.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme

/**
 * Currency placeholder icon
 *
 * @param id       currency id
 * @param modifier modifier
 */
@Composable
fun CurrencyPlaceholderIcon(id: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(color = TangemTheme.colors.icon.secondary, shape = CircleShape),
    ) {
        Text(
            text = id.firstOrNull()?.titlecase().orEmpty(),
            modifier = Modifier.padding(TangemTheme.dimens.spacing4),
            color = TangemTheme.colors.text.primary2,
            textAlign = TextAlign.Center,
            // FIXME("Incorrect typography. Replace with typography from design system")
            style = TangemTheme.typography.h3.copy(fontWeight = FontWeight.Bold),
        )
    }
}

// region preview

@Preview(showBackground = true, heightDp = 40, widthDp = 40)
@Preview(showBackground = true, heightDp = 40, widthDp = 40, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_CurrencyPlaceholderIcon() {
    TangemThemePreview {
        CurrencyPlaceholderIcon(id = "DAI")
    }
}
// endregion preview