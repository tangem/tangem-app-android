package com.tangem.core.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

/**
 * Screen for showing result
 *
 * @param resultMessage message to show
 * @param title title to show
 * @param resultColor color which will tint the round icon of the result
 * @param icon icon to show in the middle of the round icon
 * @param secondaryButtonIcon icon to show in the secondary button
 * @param secondaryButtonText label of the secondary button
 * @param onSecondaryButtonClick  action on clicking secondary button
 * @param onButtonClick  action on clicking "Done" button
 *
 * @see <a href =
 * "https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=1123%3A3863&t=wwR84h5IsMaMsDhq-1"
 * >Figma component</a>
 */
@Composable
fun ResultScreenContent(
    resultMessage: AnnotatedString,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes title: Int = R.string.common_success,
    resultColor: Color = TangemTheme.colors.icon.accent,
    @DrawableRes icon: Int = R.drawable.ic_check_24,
    @DrawableRes secondaryButtonIcon: Int? = null,
    @StringRes secondaryButtonText: Int? = null,
    onSecondaryButtonClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(TangemTheme.colors.background.secondary)
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing32,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SpacerHHalf()
        SuccessImage(resultColor = resultColor, icon = icon)
        SpacerH50()
        Text(
            text = stringResource(id = title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH12()
        Text(
            text = resultMessage,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerHHalf()
        if (onSecondaryButtonClick != null && secondaryButtonText != null) {
            SecondaryButtonForResultScreen(
                secondaryButtonText = secondaryButtonText,
                secondaryButtonIcon = secondaryButtonIcon,
                onSecondaryButtonClick = onSecondaryButtonClick,
            )
            SpacerH12()
        }
        PrimaryButton(
            text = stringResource(id = R.string.common_close),
            modifier = Modifier
                .fillMaxWidth(),
            onClick = { onButtonClick() },
        )
    }
}

@Composable
fun SuccessImage(
    resultColor: Color,
    @DrawableRes icon: Int,
) {
    Box(
        modifier = Modifier
            .background(
                color = resultColor.copy(alpha = 0.2f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(TangemTheme.dimens.spacing24)
                .background(
                    color = resultColor,
                    shape = CircleShape,
                )
                .height(TangemTheme.dimens.size93)
                .width(TangemTheme.dimens.size93),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = TangemTheme.colors.icon.primary2,
                modifier = Modifier.size(TangemTheme.dimens.size40),
            )
        }
    }
}

@Composable
private fun SecondaryButtonForResultScreen(
    @StringRes secondaryButtonText: Int,
    onSecondaryButtonClick: () -> Unit,
    @DrawableRes secondaryButtonIcon: Int? = null,
) {
    if (secondaryButtonIcon != null) {
        SecondaryButtonIconLeft(
            text = stringResource(id = secondaryButtonText),
            icon = painterResource(id = secondaryButtonIcon),
            onClick = onSecondaryButtonClick,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        SecondaryButton(
            text = stringResource(id = secondaryButtonText),
            onClick = onSecondaryButtonClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// region preview

@Composable
private fun SuccessScreenPreview() {
    ResultScreenContent(
        resultMessage = AnnotatedString("Swap of 1 000 DAI to 1 131,46 MATIC"),
        secondaryButtonText = R.string.swapping_success_view_explorer_button_title,
        onSecondaryButtonClick = {},
        onButtonClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview_SuccessScreenContent_InLightTheme() {
    TangemTheme(isDark = false) {
        SuccessScreenPreview()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_SuccessScreenContent_InDarkTheme() {
    TangemTheme(isDark = true) {
        SuccessScreenPreview()
    }
}

// endregion preview
