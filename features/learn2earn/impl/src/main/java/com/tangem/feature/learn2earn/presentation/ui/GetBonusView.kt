package com.tangem.feature.learn2earn.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemTypography
import com.tangem.feature.learn2earn.impl.R
import com.tangem.feature.learn2earn.presentation.ui.component.GradientCircle
import com.tangem.feature.learn2earn.presentation.ui.state.MainScreenState

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun GetBonusView(state: MainScreenState, modifier: Modifier = Modifier) {
    if (!state.isVisible) return

    val shape = TangemTheme.shapes.roundedCornersMedium
    Box(
        modifier = modifier
            .clickable(onClick = state.onClick)
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
            Box(modifier = Modifier.padding(horizontal = TangemTheme.dimens.size16)) {
                Image(
                    painter = painterResource(id = R.drawable.img_1inch_logo_42_40),
                    alpha = state.logoState.alpha,
                    contentDescription = null,
                )
                if (state.showProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(size = TangemTheme.dimens.size28),
                        color = TangemColorPalette.White,
                        strokeWidth = TangemTheme.dimens.size2,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(end = TangemTheme.dimens.size40),
            ) {
                Text(
                    text = state.description.title.resolveReference(),
                    style = TangemTypography.body1,
                    color = TangemTheme.colors.text.primary2,
                )
                SpacerH4()
                Text(
                    text = state.description.subtitle.resolveReference(),
                    style = TangemTypography.caption,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
        Image(
            modifier = Modifier
                .size(TangemTheme.dimens.size40)
                .padding(end = TangemTheme.dimens.size16)
                .align(Alignment.CenterEnd),
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

@Preview
@Composable
private fun GetBonusViewPreview_Light(@PreviewParameter(GetBonusViewPreviewProvider::class) state: MainScreenState) {
    TangemTheme(
        isDark = false,
    ) {
        GetBonusView(state)
    }
}

@Preview
@Composable
private fun GetBonusViewPreview_Dark(@PreviewParameter(GetBonusViewPreviewProvider::class) state: MainScreenState) {
    TangemTheme(
        isDark = true,
    ) {
        GetBonusView(state)
    }
}

private class GetBonusViewPreviewProvider : CollectionPreviewParameterProvider<MainScreenState>(
    collection = listOf(
        MainScreenState(
            isVisible = true,
            onClick = {},
            description = MainScreenState.Description.Learn(1),
            logoState = MainScreenState.LogoState.Idle,
            showProgress = false,
        ),
        MainScreenState(
            isVisible = true,
            onClick = {},
            description = MainScreenState.Description.Learn(2),
            logoState = MainScreenState.LogoState.InProgress,
            showProgress = true,
        ),
        MainScreenState(
            isVisible = true,
            onClick = {},
            description = MainScreenState.Description.GetBonus,
            logoState = MainScreenState.LogoState.Idle,
            showProgress = false,
        ),
    ),
)