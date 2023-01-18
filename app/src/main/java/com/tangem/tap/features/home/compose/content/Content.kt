package com.tangem.tap.features.home.compose.content

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH32
import com.tangem.tap.features.home.compose.StoriesBottomImageAnimation
import com.tangem.tap.features.home.compose.StoriesTextAnimation
import com.tangem.wallet.R

@Composable
fun StoriesRevolutionaryWallet(stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResource(id = R.string.story_awe_title),
                subtitleText = stringResource(id = R.string.story_awe_description).annotated(),
                isDarkBackground = true,
            )
        },
        bottomContent = {
            StoriesBottomImageAnimation(
                totalDuration = stepDuration,
                firstStepDuration = 300,
            ) { modifier ->
                StoriesImage(
                    modifier = modifier,
                    drawableResId = R.drawable.revolutionary_wallet,
                    isDarkBackground = true,
                )
            }
        },
    )
}

@Composable
fun StoriesUltraSecureBackup(isPaused: Boolean, stepDuration: Int) {
    val subtitleText = buildAnnotatedString {
        append(stringResource(id = R.string.story_backup_description_1))
        append(" ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(id = R.string.story_backup_description_2_bold))
        }
        append(" ")
        append(stringResource(id = R.string.story_backup_description_3))
    }
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResource(id = R.string.story_backup_title),
                subtitleText = subtitleText,
                isDarkBackground = false,
            )
        },
        bottomContent = {
            FloatingCardsContent(isPaused, stepDuration)
        },
    )
}

@Composable
fun StoriesCurrencies(isPaused: Boolean, stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResource(id = R.string.story_currencies_title),
                subtitleText = stringResource(id = R.string.story_currencies_description).annotated(),
                isDarkBackground = false,
            )
        },
        bottomContent = {
            StoriesCurrenciesContent(paused = isPaused, duration = stepDuration)
        },
    )
}

@Composable
fun StoriesWeb3(isPaused: Boolean, stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResource(id = R.string.story_web3_title),
                subtitleText = stringResource(id = R.string.story_web3_description).annotated(),
                isDarkBackground = false,
            )
        },
        bottomContent = {
            StoriesWeb3Content(paused = isPaused, duration = stepDuration)
        },
    )
}

@Composable
fun StoriesWalletForEveryone(stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResource(id = R.string.story_finish_title),
                subtitleText = stringResource(id = R.string.story_finish_description).annotated(),
                isDarkBackground = true,
            )
        },
        bottomContent = {
            StoriesBottomImageAnimation(
                totalDuration = stepDuration,
                firstStepDuration = 500,
            ) { modifier ->
                StoriesImage(
                    modifier = modifier,
                    drawableResId = R.drawable.wallet_for_everyone,
                    isDarkBackground = true,
                )
            }
        },
    )
}

@Composable
private fun SplitContent(
    topContent: @Composable () -> Unit,
    bottomContent: @Composable () -> Unit,
) {
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
private fun TopContent(
    titleText: String,
    subtitleText: AnnotatedString,
    isDarkBackground: Boolean,
) {
    SpacerH32()
    StoriesTitleText(
        text = titleText,
        isDarkBackground = isDarkBackground,
    )
    SpacerH16()
    StoriesSubtitleText(
        subtitleText = subtitleText,
    )
    SpacerH32()
}

@Suppress("MagicNumber")
@Composable
private fun StoriesTitleText(
    text: String,
    isDarkBackground: Boolean,
) {
    StoriesTextAnimation(
        slideInDuration = 500,
        slideInDelay = 150,
    ) { modifier ->
        Text(
            modifier = modifier
                .padding(start = 40.dp, end = 40.dp),
            text = text,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDarkBackground) Color.White else Color(0xFF090E13),
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun StoriesSubtitleText(subtitleText: AnnotatedString) {
    val color = Color(0xFFA6AAAD)

    StoriesTextAnimation(
        slideInDuration = 500,
        slideInDelay = 400,
    ) { modifier ->
        Text(
            modifier = modifier
                .padding(start = 40.dp, end = 40.dp),
            fontWeight = FontWeight.Normal,
            text = subtitleText,
            fontSize = 20.sp,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StoriesImage(
    @DrawableRes drawableResId: Int,
    isDarkBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = drawableResId),
        contentDescription = null,
        contentScale = if (isDarkBackground) ContentScale.Inside else ContentScale.FillWidth,
        modifier = modifier.fillMaxWidth(),
    )
}

private fun String.annotated(): AnnotatedString {
    val source = this
    return buildAnnotatedString { append(source) }
}

@Preview
@Composable
private fun RevolutionaryWalletPreview() {
    StoriesRevolutionaryWallet(6000)
}

@Preview
@Composable
private fun UltraSecureBackupPreview() {
    StoriesUltraSecureBackup(false, 6000)
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
