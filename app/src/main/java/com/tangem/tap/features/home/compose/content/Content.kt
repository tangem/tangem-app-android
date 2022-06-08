package com.tangem.tap.features.home.compose.content

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tangem.tangem_sdk_new.extensions.dpToPx
import com.tangem.tap.common.compose.SpacerH
import com.tangem.tap.common.compose.SpacerH16
import com.tangem.tap.common.compose.extensions.toAndroidGraphicsColor
import com.tangem.tap.features.home.compose.StoriesBottomImageAnimation
import com.tangem.tap.features.home.compose.StoriesTextAnimation
import com.tangem.wallet.R

@Composable
fun StoriesRevolutionaryWallet(stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResource(id = R.string.story_awe_title),
                subtitleText = stringResource(id = R.string.story_awe_description),
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
        }
    )
}

@Composable
fun StoriesUltraSecureBackup(isPaused: Boolean, stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResource(id = R.string.story_backup_title),
                subtitleText = stringResource(id = R.string.story_backup_description),
                isDarkBackground = false,
            )
        },
        bottomContent = {
            FloatingCardsContent(isPaused, stepDuration)
        }
    )
}

@Composable
fun StoriesCurrencies(isPaused: Boolean, stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResource(id = R.string.story_currencies_title),
                subtitleText = stringResource(id = R.string.story_currencies_description),
                isDarkBackground = false,
            )
        },
        bottomContent = {
            StoriesCurrenciesContent(paused = isPaused, duration = stepDuration)
        }
    )
}

@Composable
fun StoriesWeb3(isPaused: Boolean, stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResource(id = R.string.story_web3_title),
                subtitleText = stringResource(id = R.string.story_web3_description),
                isDarkBackground = false,
            )
        },
        bottomContent = {
            StoriesWeb3Content(paused = isPaused, duration = stepDuration)
        }
    )
}

@Composable
fun StoriesWalletForEveryone(stepDuration: Int) {
    SplitContent(
        topContent = {
            TopContent(
                titleText = stringResource(id = R.string.story_finish_title),
                subtitleText = stringResource(id = R.string.story_finish_description),
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
        }
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
        verticalArrangement = Arrangement.Top
    ) {
        topContent()
        bottomContent()
    }
}

@Composable
private fun TopContent(
    titleText: String,
    subtitleText: String,
    isDarkBackground: Boolean,
    subtitleTextId: Int? = null,
) {
    SpacerH(32.dp)
    StoriesTitleText(
        text = titleText,
        isDarkBackground = isDarkBackground,
    )
    SpacerH16()
    StoriesSubtitleText(
        subtitleText = subtitleText,
        subtitleTextId = subtitleTextId,
    )
    SpacerH(32.dp)
}

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
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StoriesSubtitleText(
    subtitleText: String,
    subtitleTextId: Int?,
) {
    val color = Color(0xFFA6AAAD)

    StoriesTextAnimation(
        slideInDuration = 500,
        slideInDelay = 400,
    ) { modifier ->
        val internalModifier = modifier
            .padding(start = 40.dp, end = 40.dp)

        if (subtitleTextId == null) {
            Text(
                text = subtitleText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                modifier = internalModifier,
                color = color,
                textAlign = TextAlign.Center
            )
        } else {
            HtmlText(
                modifier = internalModifier,
                stringResId = subtitleTextId,
            ) { context ->
                val padding = context.dpToPx(40f).toInt()
                TextView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setPadding(padding, 0, padding, 0)
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    typeface = Typeface.DEFAULT
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    setTextColor(color.toAndroidGraphicsColor())
                }
            }
        }
    }
}

@Composable
private fun HtmlText(
    stringResId: Int,
    modifier: Modifier = Modifier,
    factory: (Context) -> TextView
) {
    AndroidView(factory, modifier) { it.text = it.context.getText(stringResId) }
}

@Composable
private fun StoriesImage(
    modifier: Modifier = Modifier,
    @DrawableRes drawableResId: Int,
    isDarkBackground: Boolean,
) {
    Image(
        painter = painterResource(id = drawableResId),
        contentDescription = null,
        contentScale = if (isDarkBackground) ContentScale.Inside else ContentScale.FillWidth,
        modifier = modifier.fillMaxWidth()
    )
}