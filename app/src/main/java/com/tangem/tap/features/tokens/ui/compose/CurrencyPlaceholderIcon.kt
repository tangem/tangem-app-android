package com.tangem.tap.features.tokens.ui.compose

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CurrencyPlaceholderIcon(id: String) {
    val letterColor: Color = Color.White
    val circleColor: Color = Color.Black

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(circleColor, shape = CircleShape)
    ) {
        Text(
            text = id.firstOrNull()?.titlecase() ?: "",
            textAlign = TextAlign.Center,
            color = letterColor,
            modifier = Modifier.padding(4.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}