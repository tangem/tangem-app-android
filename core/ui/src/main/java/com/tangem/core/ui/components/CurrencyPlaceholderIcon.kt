package com.tangem.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.res.TangemTheme

@Composable
fun CurrencyPlaceholderIcon(id: String, modifier: Modifier = Modifier) {
    val letterColor: Color = TangemTheme.colors.text.primary2
    val circleColor: Color = TangemTheme.colors.icon.secondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(circleColor, shape = CircleShape),
    ) {
        Text(
            text = id.firstOrNull()?.titlecase() ?: "",
            textAlign = TextAlign.Center,
            color = letterColor,
            modifier = Modifier.padding(TangemTheme.dimens.spacing4),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// region preview

@Preview(showBackground = true, heightDp = 40, widthDp = 40)
@Composable
private fun Preview_CurrencyPlaceholderIcon_InLightTheme() {
    TangemTheme(isDark = false) {
        CurrencyPlaceholderIcon(id = "DAI")
    }
}

@Preview(showBackground = true, heightDp = 40, widthDp = 40)
@Composable
private fun Preview_CurrencyPlaceholderIcon_InDarkTheme() {
    TangemTheme(isDark = true) {
        CurrencyPlaceholderIcon(id = "DAI")
    }
}

// endregion preview
