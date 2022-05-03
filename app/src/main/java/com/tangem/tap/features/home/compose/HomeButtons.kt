package com.tangem.tap.features.home.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.tap.common.compose.SpacerS8
import com.tangem.wallet.R

@Composable
fun HomeButtons(
    isDarkBackground: Boolean, modifier: Modifier = Modifier,
    onScanButtonClick: () -> Unit,
    onShopButtonClick: () -> Unit
) {
    val darkColorBackground = Color(0xFF26292E)
    val darkColorButton = Color(0xFF080C10)

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
    ) {
        Button(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            onClick = onScanButtonClick,
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = if (isDarkBackground) darkColorBackground else Color.White,
                contentColor = if (isDarkBackground) Color.White else darkColorButton
            )
        ) {
            Text(
                text = stringResource(id = R.string.home_button_scan),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        SpacerS8()
        Button(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            onClick = onShopButtonClick,
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = if (isDarkBackground) Color.White else darkColorBackground,
                contentColor = if (isDarkBackground) darkColorButton else Color.White
            )
        ) {
            Text(
                text = stringResource(id = R.string.home_button_order),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}