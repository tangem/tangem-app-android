package com.tangem.feature.learn2earn.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemTypography
import com.tangem.feature.learn2earn.impl.R
import com.tangem.feature.learn2earn.presentation.ui.component.GradientCircle

/**
[REDACTED_AUTHOR]
 */
@Composable
fun GetBonusAlert(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = TangemTheme.shapes.roundedCornersMedium
    Box(
        modifier = modifier
            .height(TangemTheme.dimens.size96)
            .background(
                color = TangemTheme.colors.background.action,
                shape = shape,
            ),
    ) {
        ContentBackground(shape)
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.size16),
                painter = painterResource(id = R.drawable.img_1inch_logo_42_40),
                contentDescription = null,
            )
            Column(
                modifier = Modifier.padding(end = TangemTheme.dimens.size40),
            ) {
                Text(
                    text = stringResource(id = R.string.main_learn_title),
                    style = TangemTypography.body1,
                    color = TangemTheme.colors.text.primary2,
                )
                Text(
                    text = stringResource(id = R.string.main_learn_subtitle),
                    style = TangemTypography.caption,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
        Image(
            modifier = Modifier
                .size(TangemTheme.dimens.size40)
                .padding(end = TangemTheme.dimens.size16)
                .align(Alignment.CenterEnd)
                .clickable(onClick = onClick),
            painter = painterResource(id = R.drawable.ic_chevron_right_24),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.Gray),
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun ContentBackground(shape: Shape) {
    Box(
        modifier = Modifier
            .clip(shape)
            .fillMaxSize(),
    ) {
        GradientCircle(
            size = 340.dp,
            offsetX = (-103).dp,
            offsetY = 16.dp,
            startColor = Color(0xFF1D5DC7),
            endColor = Color(0x000B173D),
        )
        GradientCircle(
            size = 268.dp,
            offsetX = 90.dp,
            offsetY = 0.dp,
            startColor = Color(0xD8FF0CCA),
            endColor = Color(0x000B0E17),
        )
    }
    Box(
        modifier = Modifier
            .background(
                color = TangemTheme.colors.background.action.copy(alpha = 0.7f),
                shape = shape,
            )
            .fillMaxSize(),
    )
}