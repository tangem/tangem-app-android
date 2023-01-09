package com.tangem.tap.features.home.compose.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.SpacerS8
import com.tangem.wallet.R

@Composable
fun HomeButtons(
    modifier: Modifier = Modifier,
    isDarkBackground: Boolean,
    btnScanStateInProgress: Boolean,
    onScanButtonClick: () -> Unit,
    onShopButtonClick: () -> Unit,
) {
    val darkColorBackground = Color(0xFF26292E)
    val darkColorButton = Color(0xFF080C10)

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier,
    ) {
        ProgressButton(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            inProgress = btnScanStateInProgress,
            backgroundColor = if (isDarkBackground) darkColorBackground else Color.White,
            contentColor = if (isDarkBackground) Color.White else darkColorButton,
            onClick = onScanButtonClick,
            progressContent = {
                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = if (isDarkBackground) Color.White else darkColorBackground,
                    strokeWidth = 3.dp,
                )
            },
            content = {
                Text(
                    text = stringResource(id = R.string.welcome_unlock_card),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            },
        )
        SpacerS8()
        Button(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            onClick = onShopButtonClick,
            enabled = !btnScanStateInProgress,
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = if (isDarkBackground) Color.White else darkColorBackground,
                contentColor = if (isDarkBackground) darkColorButton else Color.White,
            ),
        ) {
            Text(
                text = stringResource(id = R.string.home_button_order),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun ProgressButton(
    modifier: Modifier,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    inProgress: Boolean,
    progressContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
        ),
    ) {
        if (progressContent != null && inProgress) {
            progressContent()
        } else {
            content()
        }
    }
}
