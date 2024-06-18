package com.tangem.features.disclaimer.impl.presentation.ui

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.disclaimer.impl.R
import com.tangem.features.disclaimer.impl.presentation.state.DisclaimerState
import com.tangem.features.disclaimer.impl.presentation.state.DummyDisclaimer

@Composable
internal fun DisclaimerScreen(state: DisclaimerState, onBackClick: () -> Unit) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    Box(
        modifier = Modifier
            .background(TangemColorPalette.Dark6)
            .statusBarsPadding(),
    ) {
        Column {
            AppBarWithAdditionalButtons(
                text = resourceReference(R.string.disclaimer_title),
                startButton = AdditionalButton(
                    iconRes = R.drawable.ic_back_24,
                    onIconClicked = onBackClick,
                ).takeIf { state.isTosAccepted },
            )
            DisclaimerContent(state.url)
            Spacer(
                modifier = Modifier
                    .background(TangemColorPalette.Dark6)
                    .height(
                        height = if (!state.isTosAccepted) {
                            bottomBarHeight
                        } else {
                            bottomBarHeight + TangemTheme.dimens.size64
                        },
                    ),
            )
        }

        if (!state.isTosAccepted) {
            BottomFade(Modifier.align(Alignment.BottomCenter), backgroundColor = TangemColorPalette.Dark6)
            DisclaimerButton(state.onAccept)
        } else {
            NavigationBar3ButtonsScrim()
        }
    }
}

@Composable
private fun ColumnScope.DisclaimerContent(url: String) {
    val progressState = remember { mutableStateOf(ProgressState.Loading) }
    val webClient = remember { DisclaimerWebViewClient(progressState) }
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
                    setBackgroundColor(TangemColorPalette.Dark6.toArgb())
                    settings.allowFileAccess = false
                    settings.javaScriptEnabled = false

                    overScrollMode = View.OVER_SCROLL_NEVER
                    webViewClient = webClient
                    loadUrl(url)
                }
            },
        )

        when (progressState.value) {
            ProgressState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        color = TangemTheme.colors.icon.informative,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(TangemTheme.dimens.spacing8),
                    )
                }
            }
            ProgressState.Error -> Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TangemTheme.dimens.spacing16)
                        .align(Alignment.Center),
                ) {
                    Text(
                        text = stringResource(id = R.string.disclaimer_error_loading),
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.constantWhite,
                    )
                    PrimaryButton(
                        text = stringResource(R.string.common_retry),
                        onClick = { webClient.reset() },
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
    val shouldAskPushPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS).status.isGranted
    } else {
        true
    }
    PrimaryButton(
        text = stringResource(id = R.string.common_accept),
        onClick = { onAccept(shouldAskPushPermission) },
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
