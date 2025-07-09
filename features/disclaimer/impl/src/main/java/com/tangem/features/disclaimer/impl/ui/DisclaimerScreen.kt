package com.tangem.features.disclaimer.impl.ui

import android.content.res.Configuration
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.DisclaimerScreenTestTags
import com.tangem.core.ui.webview.applySafeSettings
import com.tangem.features.disclaimer.impl.R
import com.tangem.features.disclaimer.impl.entity.DisclaimerUM
import com.tangem.features.disclaimer.impl.entity.DummyDisclaimer
import com.tangem.features.disclaimer.impl.local.localTermsOfServices
import com.tangem.features.pushnotifications.api.utils.getPushPermissionOrNull
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.withDebounce
import java.nio.charset.StandardCharsets

@Composable
internal fun DisclaimerScreen(state: DisclaimerUM) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    val bottomPadding = if (state.isTosAccepted) {
        bottomBarHeight + TangemTheme.dimens.size16
    } else {
        bottomBarHeight + TangemTheme.dimens.size64
    }
    val backgroundColor = TangemTheme.colors.background.primary
    val (textColor, iconColor) = TangemTheme.colors.text.primary1 to TangemTheme.colors.icon.primary1
    Box(
        modifier = Modifier
            .background(backgroundColor)
            .statusBarsPadding()
            .testTag(DisclaimerScreenTestTags.SCREEN_CONTAINER),
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = bottomPadding)
                .fillMaxSize(),
        ) {
            TangemTopAppBar(
                title = resourceReference(R.string.disclaimer_title),
                startButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_back_24,
                    onIconClicked = state.popBack,
                ).takeIf { state.isTosAccepted },
                titleAlignment = Alignment.CenterHorizontally,
                textColor = textColor,
                iconTint = iconColor,
            )
            DisclaimerContent(state.url)
        }

        if (!state.isTosAccepted) {
            BottomFade(
                modifier = Modifier.align(Alignment.BottomCenter),
                backgroundColor = backgroundColor,
            )
            DisclaimerButton(state.onAccept)
        } else {
            NavigationBar3ButtonsScrim()
        }
    }
}

@Composable
private fun DisclaimerContent(url: String) {
    val backgroundColor = TangemTheme.colors.background.primary

    val loadingTimeoutJobHolder = remember { JobHolder() }
    var isLoading by remember { mutableStateOf(true) }

    val webViewState = rememberWebViewState(url)
    val webViewNavigator = rememberWebViewNavigator()
    val webViewClient = remember {
        DisclaimerWebViewClient(
            onLoadingFinished = {
                loadingTimeoutJobHolder.cancel()
                isLoading = false
            },
        )
    }

    Box {
        WebView(
            state = webViewState,
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.primary),
            captureBackPresses = false,
            navigator = webViewNavigator,
            onCreated = WebView::applySafeSettings,
            client = webViewClient,
        )

        AnimatedVisibility(
            visible = isLoading,
            label = "Loading state change animation",
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
            ) {
                CircularProgressIndicator(
                    color = TangemTheme.colors.icon.informative,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(TangemTheme.dimens.spacing8),
                )
            }
        }
    }

    LaunchedEffect(loadingTimeoutJobHolder) {
        withDebounce(jobHolder = loadingTimeoutJobHolder, timeMillis = 30_000) {
            webViewNavigator.stopLoading()

            webViewNavigator.loadLocalToS()
        }
    }

    LaunchedEffect(webViewState.errorsForCurrentRequest) {
        if (webViewState.errorsForCurrentRequest.isNotEmpty()) {
            webViewNavigator.loadLocalToS()
        }
    }
}

private fun WebViewNavigator.loadLocalToS() {
    loadHtml(
        html = localTermsOfServices,
        mimeType = "text/html",
        encoding = StandardCharsets.UTF_8.name(),
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun BoxScope.DisclaimerButton(onAccept: (Boolean) -> Unit) {
    val isPermissionGranted = getPushPermissionOrNull()?.let { permission ->
        rememberPermissionState(permission = permission).status.isGranted
    } ?: true
    PrimaryButton(
        text = stringResourceSafe(id = R.string.common_accept),
        onClick = { onAccept(!isPermissionGranted) },
        colors = ButtonColors(
            containerColor = TangemColorPalette.Light4,
            contentColor = TangemColorPalette.Dark6,
            disabledContainerColor = TangemColorPalette.Light4,
            disabledContentColor = TangemColorPalette.Dark6,
        ),
        modifier = Modifier
            .testTag(DisclaimerScreenTestTags.ACCEPT_BUTTON)
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            )
            .fillMaxWidth(),
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DisclaimerScreen_Preview() {
    TangemThemePreview {
        DisclaimerScreen(state = DummyDisclaimer.state)
    }
}
// endregion