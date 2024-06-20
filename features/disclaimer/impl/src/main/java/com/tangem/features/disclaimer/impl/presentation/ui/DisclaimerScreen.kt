package com.tangem.features.disclaimer.impl.presentation.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithAdditionalButtons
import com.tangem.core.ui.components.appbar.models.AdditionalButton
import com.tangem.core.ui.components.buttons.common.TangemButtonColors
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.disclaimer.impl.R
import com.tangem.features.disclaimer.impl.presentation.state.DisclaimerState
import com.tangem.features.disclaimer.impl.presentation.state.DummyDisclaimer
import com.tangem.features.pushnotifications.api.utils.getPushPermissionOrNull

@Composable
internal fun DisclaimerScreen(state: DisclaimerState, onBackClick: () -> Unit) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    val bottomPadding = if (state.isTosAccepted) {
        bottomBarHeight + TangemTheme.dimens.size16
    } else {
        bottomBarHeight + TangemTheme.dimens.size64
    }
    val backgroundColor = if (state.isTosAccepted) TangemTheme.colors.background.primary else TangemColorPalette.Dark6
    val (textColor, iconColor) = if (state.isTosAccepted) {
        TangemTheme.colors.text.primary1 to TangemTheme.colors.icon.primary1
    } else {
        TangemColorPalette.Light4 to TangemColorPalette.Light4
    }
    Box(
        modifier = Modifier
            .background(backgroundColor)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(bottom = bottomPadding)) {
            AppBarWithAdditionalButtons(
                text = resourceReference(R.string.disclaimer_title),
                startButton = AdditionalButton(
                    iconRes = R.drawable.ic_back_24,
                    onIconClicked = onBackClick,
                ).takeIf { state.isTosAccepted },
                textColor = textColor,
                iconColor = iconColor,
            )
            DisclaimerContent(state.url, state.isTosAccepted)
        }

        if (!state.isTosAccepted) {
            BottomFade(Modifier.align(Alignment.BottomCenter), backgroundColor = backgroundColor)
            DisclaimerButton(state.onAccept)
        } else {
            NavigationBar3ButtonsScrim()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DisclaimerContent(url: String, isTosAccepted: Boolean) {
    val progressState = remember { mutableStateOf(ProgressState.Loading) }
    val webClient = remember { DisclaimerWebViewClient(progressState) }
    val transparent = Color.Transparent
    val backgroundColor = if (isTosAccepted) TangemTheme.colors.background.primary else TangemColorPalette.Dark6
    Box(
        modifier = Modifier,
    ) {
        AndroidView(
            factory = {
                WebView(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(transparent.toArgb())
                    settings.allowFileAccess = false
                    // to inject css style to display only in dark theme
                    settings.javaScriptEnabled = !isTosAccepted
                    overScrollMode = View.OVER_SCROLL_NEVER
                    webViewClient = webClient

                    clearHistory()
                    clearFormData()
                    clearCache(true)

                    loadUrl(url)
                }
            },
        )

        when (progressState.value) {
            ProgressState.Loading -> {
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
            else -> Unit
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun BoxScope.DisclaimerButton(onAccept: (Boolean) -> Unit) {
    val shouldAskPushPermission = getPushPermissionOrNull()?.let { permission ->
        rememberPermissionState(permission = permission).status.isGranted
    } ?: true
    PrimaryButton(
        text = stringResource(id = R.string.common_accept),
        onClick = { onAccept(shouldAskPushPermission) },
        colors = TangemButtonColors(
            backgroundColor = TangemColorPalette.Light4,
            contentColor = TangemColorPalette.Dark6,
            disabledBackgroundColor = TangemColorPalette.Light4,
            disabledContentColor = TangemColorPalette.Dark6,
        ),
        modifier = Modifier
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
        DisclaimerScreen(state = DummyDisclaimer.state, onBackClick = {})
    }
}
// endregion