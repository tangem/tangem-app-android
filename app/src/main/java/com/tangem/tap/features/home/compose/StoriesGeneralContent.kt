package com.tangem.tap.features.home.compose

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.tangem.tap.common.compose.SpacerS16
import com.tangem.tap.common.compose.SpacerS24
import com.tangem.tap.common.extensions.compose.toAndroidGraphicsColor
import com.tangem.wallet.R

@Composable
fun StoriesGeneralContent(
    titleText: String,
    subtitleText: String,
    imageSource: Int?,
    isDarkBackground: Boolean,
    subtitleTextId: Int? = null,
    imageComposable: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {

        Text(
            text = titleText,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(start = 40.dp, end = 40.dp),
            color = if (isDarkBackground) Color.White else Color(0xFF090E13),
            textAlign = TextAlign.Center
        )

        SpacerS16()

        SubtitleText(subtitleText, subtitleTextId)

        SpacerS24()

        if (imageSource != null) {
            Image(
                painter = painterResource(id = imageSource),
                contentDescription = null,
                contentScale = if (isDarkBackground) ContentScale.Inside else ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
        imageComposable?.invoke()
    }
}

@Composable
fun SubtitleText(subtitleText: String, subtitleTextId: Int?) {
    val color = Color(0xFFA6AAAD)

    if (subtitleTextId == null) {
        Text(
            text = subtitleText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(start = 40.dp, end = 40.dp),
            color = color,
            textAlign = TextAlign.Center
        )
    } else {
        HtmlText(subtitleTextId) { context ->
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

@Composable
fun StoriesRevolutionaryWallet() {
    StoriesGeneralContent(
        titleText = stringResource(id = R.string.story_awe_title),
        subtitleText = stringResource(id = R.string.story_awe_description),
        imageSource = R.drawable.revolutionary_wallet,
        isDarkBackground = true
    )
}

@Composable
fun StoriesUltraSecureBackup() {
    StoriesGeneralContent(
        titleText = stringResource(id = R.string.story_backup_title),
        subtitleText = stringResource(id = R.string.story_backup_description),
        imageSource = R.drawable.floating_cards,
        subtitleTextId = R.string.story_backup_description,
        isDarkBackground = false
    )
}

@Composable
fun StoriesThousandsOfCurrencies() {
    StoriesGeneralContent(
        titleText = stringResource(id = R.string.story_currencies_title),
        subtitleText = stringResource(id = R.string.story_currencies_description),
        imageSource = R.drawable.thousands_of_currencies,
        isDarkBackground = false
    )
}

@Composable
fun StoriesWeb3() {
    StoriesGeneralContent(
        titleText = stringResource(id = R.string.story_web3_title),
        subtitleText = stringResource(id = R.string.story_web3_description),
        imageSource = R.drawable.web_3,
        isDarkBackground = false
    )
}

@Composable
fun StoriesWalletForEveryone() {
    StoriesGeneralContent(
        titleText = stringResource(id = R.string.story_finish_title),
        subtitleText = stringResource(id = R.string.story_finish_description),
        imageSource = R.drawable.wallet_for_everyone,
        isDarkBackground = true
    )
}

@Composable
fun HtmlText(
    stringResId: Int,
    modifier: Modifier = Modifier,
    factory: (Context) -> TextView
) {
    AndroidView(factory, modifier) { it.text = it.context.getText(stringResId) }
}