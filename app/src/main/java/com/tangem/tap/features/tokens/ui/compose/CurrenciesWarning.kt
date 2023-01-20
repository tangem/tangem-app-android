package com.tangem.tap.features.tokens.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.wallet.R

@Suppress("MagicNumber")
@Composable
fun CurrenciesWarning() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(16.dp)
            .background(
                color = Color(0xFFF2F2F2),
                shape = RoundedCornerShape(10.dp),
            ),
    ) {
        val warning = stringResource(id = R.string.alert_manage_tokens_addresses_message)
        val end = warning.indexOf(" ")
        val spanStyles = listOf(
            AnnotatedString.Range(
                SpanStyle(fontWeight = FontWeight.Bold),
                start = 0,
                end = end,
            ),
        )
        Text(
            text = AnnotatedString(text = warning, spanStyles = spanStyles),
            textAlign = TextAlign.Start,
            color = Color(0xFF848488),
            modifier = Modifier.padding(
                top = 8.dp,
                bottom = 8.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}
