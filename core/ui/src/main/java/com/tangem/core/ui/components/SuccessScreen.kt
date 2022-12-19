package com.tangem.core.ui.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

/**
 * Screen for showing success
 *
 * @param successMessage
 * @param onButtonClick  action on clicking "Done" button
 *
 * @see <a href = "https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=1123%3A3863&t=wwR84h5IsMaMsDhq-1"
 * >Figma component</a>
 */
@Composable
fun SuccessScreenContent(
    modifier: Modifier = Modifier,
    successMessage: String,
    onButtonClick: () -> Unit,
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
        SuccessImage()
        SpacerH50()
        Text(
            text = stringResource(id = R.string.common_success),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH12()
        Text(
            text = successMessage,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerHHalf()
        PrimaryButton(
            text = stringResource(id = R.string.common_done),
            modifier = Modifier
                .fillMaxWidth(),
            onClick = { onButtonClick() },
        )
    }
}

@Composable
fun SuccessImage() {
    Box(
        modifier = Modifier
            .background(
                color = TangemTheme.colors.icon.accent.copy(alpha = 0.2f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(TangemTheme.dimens.spacing24)
                .background(
                    color = TangemTheme.colors.icon.accent,
                    shape = CircleShape,
                )
                .height(TangemTheme.dimens.size93)
                .width(TangemTheme.dimens.size93),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.primary2,
                modifier = Modifier.size(TangemTheme.dimens.size40),
            )
        }
    }
}

// region preview

@Composable
private fun SuccessScreenPreview() {
    SuccessScreenContent(
        successMessage = "Swap of 1 000 DAI to 1 131,46 MATIC",
        onButtonClick = {},
    )
}

@Preview(showBackground = true)
@Composable
fun Preview_SuccessScreenContent_InLightTheme() {
    TangemTheme(isDark = false) {
        SuccessScreenPreview()
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_SuccessScreenContent_InDarkTheme() {
    TangemTheme(isDark = true) {
        SuccessScreenPreview()
    }
}

// endregion preview
