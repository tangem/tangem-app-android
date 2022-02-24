package com.tangem.tap.features.home.compose

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
import com.tangem.wallet.R

@Composable
fun StoriesGeneralContent(
    titleText: String,
    subtitleText: String,
    imageSource: Int?,
    isDarkBackground: Boolean,
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

        Spacer(modifier = Modifier.size(16.dp))

        Text(
            text = subtitleText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .padding(start = 40.dp, end = 40.dp),
            color = Color(0xFFA6AAAD),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(25.dp))

        if (imageSource != null) {
            Image(
                painter = painterResource(id = imageSource),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
        imageComposable?.invoke()
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