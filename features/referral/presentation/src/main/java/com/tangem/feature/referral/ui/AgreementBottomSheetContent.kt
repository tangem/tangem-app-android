package com.tangem.feature.referral.ui

import android.view.ViewGroup.LayoutParams
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tangem.core.ui.components.appbar.AppBarWithAdditionalButtons
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.referral.presentation.R

/**
 * Bottom sheet content with referral program terms of use
 *
 * @param url link to the html page
 */
@Composable
internal fun AgreementBottomSheetContent(url: String) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxWidth()
            .height(LocalConfiguration.current.screenHeightDp.dp - TangemTheme.dimens.spacing16),
    ) {
        Hand()
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            AppBarWithAdditionalButtons(text = stringResource(id = R.string.details_referral_title))
            AgreementHtmlView(url = url)
        }
    }
}

@Composable
private fun AgreementHtmlView(url: String) {
    AndroidView(
        modifier = Modifier.background(TangemTheme.colors.background.secondary),
        factory = {
            WebView(it).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = false
                    allowFileAccess = false
                }
                loadUrl(url)
            }
        },
        update = { it.loadUrl(url) },
    )
}

@Preview
@Composable
private fun Preview_AgreementBottomSheet_InLightTheme() {
    TangemTheme(isDark = false) {
        AgreementBottomSheetContent(url = "https://tangem.com/en/")
    }
}

@Preview
@Composable
private fun Preview_AgreementBottomSheet_InDarkTheme() {
    TangemTheme(isDark = true) {
        AgreementBottomSheetContent(url = "https://tangem.com/en/")
    }
}
