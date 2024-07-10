package com.tangem.feature.referral.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.tangem.core.ui.components.appbar.AppBarWithAdditionalButtons
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.referral.presentation.R

/**
 * Bottom sheet content with referral program terms of use
 *
 * @param url link to the html page
 */
@Composable
internal fun AgreementBottomSheetContent(url: String) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AppBarWithAdditionalButtons(text = stringResource(id = R.string.details_referral_title))
        AgreementHtmlView(url = url)
        Spacer(modifier = Modifier.height(bottomBarHeight))
    }
}

@Composable
private fun AgreementHtmlView(url: String) {
    val state = rememberWebViewState(url)
    val isInPreviewMode = LocalInspectionMode.current
    WebView(
        state = state,
        modifier = Modifier.background(TangemTheme.colors.background.primary),
        captureBackPresses = false,
        onCreated = {
            if (!isInPreviewMode) {
                it.settings.apply {
                    javaScriptEnabled = false
                    allowFileAccess = false
                }
            }
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AgreementBottomSheet() {
    TangemThemePreview {
        AgreementBottomSheetContent(url = "https://tangem.com/en/")
    }
}