package com.tangem.tap.features.home.compose.content

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.home.compose.StoriesBottomImageAnimation
import com.tangem.tap.features.home.compose.StoriesTextAnimation
import com.tangem.wallet.R

@Composable
fun StoriesRevolutionaryWallet() {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResourceSafe(id = R.string.story_awe_title),
                subtitleText = stringResourceSafe(id = R.string.story_awe_description),
            )
        },
        bottomContent = {
            SpacerH32()
            StoriesImage(
                modifier = Modifier,
                drawableResId = R.drawable.img_revolutionary_wallet,
            )
        },
    )
}

@Composable
fun StoriesUltraSecureBackup(isPaused: Boolean, stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResourceSafe(id = R.string.story_backup_title),
                subtitleText = stringResourceSafe(id = R.string.story_backup_description),
            )
        },
        bottomContent = {
            SpacerH32()
            FloatingCardsContent(
                isPaused = isPaused,
                stepDuration = stepDuration,
            )
        },
    )
}

@Composable
fun StoriesCurrencies(isPaused: Boolean, stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResourceSafe(id = R.string.story_currencies_title),
                subtitleText = stringResourceSafe(id = R.string.story_currencies_description),
            )
        },
        bottomContent = {
            SpacerH32()
            StoriesCurrenciesContent(paused = isPaused, duration = stepDuration)
        },
    )
}

@Composable
fun StoriesWeb3(isPaused: Boolean, stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResourceSafe(id = R.string.story_web3_title),
                subtitleText = stringResourceSafe(id = R.string.story_web3_description),
            )
        },
        bottomContent = {
            SpacerH(TangemTheme.dimens.spacing70)
            StoriesWeb3Content(paused = isPaused, duration = stepDuration)
        },
    )
}

@Composable
fun StoriesWalletForEveryone(stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResourceSafe(id = R.string.story_finish_title),
                subtitleText = stringResourceSafe(id = R.string.story_finish_description),
            )
        },
        bottomContent = {
            SpacerH32()
            BoxWithGradient {
                StoriesBottomImageAnimation(
                    initialScale = 2.6f,
                    secondStageScale = 1.2f,
                    targetScale = 1.1f,
                    totalDuration = stepDuration,
                    firstStepDuration = 500,
                ) { modifier ->
                    StoriesImage(
                        modifier = modifier,
                        drawableResId = R.drawable.img_tangem_for_everyone,
                    )
                }
            }
        },
    )
}

@Composable
private fun SplitContent(topContent: @Composable () -> Unit, bottomContent: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        topContent()
        bottomContent()
    }
}

@Composable
private fun TopContent(titleText: String, subtitleText: String) {
    SpacerH(TangemTheme.dimens.spacing36)
    StoriesTitleText(
        text = titleText,
    )
    SpacerH16()
    StoriesSubtitleText(
        subtitleText = subtitleText,
    )
}

@Suppress("MagicNumber")
@Composable
private fun StoriesTitleText(text: String) {
    StoriesTextAnimation(
        slideInDuration = 500,
        slideInDelay = 150,
    ) { modifier ->
        Text(
            modifier = modifier
                .padding(start = 40.dp, end = 40.dp),
            text = text,
            style = TangemTheme.typography.head,
            color = TangemColorPalette.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun StoriesSubtitleText(subtitleText: String) {
    StoriesTextAnimation(
        slideInDuration = 500,
        slideInDelay = 400,
    ) { modifier ->
        Text(
            modifier = modifier
                .padding(start = 40.dp, end = 40.dp),
            text = subtitleText,
            style = TangemTheme.typography.body1,
            color = TangemColorPalette.Dark1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StoriesImage(@DrawableRes drawableResId: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = drawableResId),
        contentDescription = null,
        contentScale = ContentScale.Inside,
        modifier = modifier.fillMaxSize(),
    )
}

@Preview
@Composable
private fun RevolutionaryWalletPreview() {
    StoriesRevolutionaryWallet()
}

@Preview
@Composable
private fun UltraSecureBackupPreview() {
    StoriesUltraSecureBackup(
        false,
        6000,
    )
}

@Preview
@Composable
private fun CurrenciesPreview() {
    StoriesCurrencies(false, 6000)
}

@Preview
@Composable
private fun Web3Preview() {
    StoriesWeb3(false, 6000)
}

@Preview
@Composable
private fun WalletForEveryonePreview() {
    StoriesWalletForEveryone(6000)
}