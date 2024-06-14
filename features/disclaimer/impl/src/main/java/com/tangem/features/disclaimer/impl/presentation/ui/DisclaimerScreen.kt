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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
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
    val bottomPadding = if (state.isTosAccepted) TangemTheme.dimens.spacing16 else TangemTheme.dimens.size64
    Box(
        modifier = Modifier
            .statusBarsPadding()
            .background(TangemColorPalette.Dark6),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = bottomPadding),
        ) {
            AppBarWithAdditionalButtons(
                text = resourceReference(R.string.disclaimer_title),
                startButton = AdditionalButton(
                    iconRes = R.drawable.ic_back_24,
                    onIconClicked = onBackClick,
                ).takeIf { state.isTosAccepted },
            )
            DisclaimerContent(state.url)
        }
        if (!state.isTosAccepted) {
            DisclaimerButton(state.onAccept)
        }
    }
}

@Composable
private fun ColumnScope.DisclaimerContent(url: String) {
    val transparent = Color.Transparent

    val progressState = remember { mutableStateOf(ProgressState.Loading) }
    val webClient = remember { DisclaimerWebViewClient(progressState) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
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
                    settings.javaScriptEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    webViewClient = webClient
                    loadUrl(url)
                }
            },
        )

        when (progressState.value) {
            ProgressState.Loading -> CircularProgressIndicator(
                color = TangemTheme.colors.icon.informative,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(TangemTheme.dimens.spacing8),
            )
            ProgressState.Error -> Column(
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
    val bottomInsetsPx = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(TangemTheme.dimens.size110 + bottomInsetsPx.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TangemTheme.colors.background.primary.copy(alpha = 0f),
                        TangemTheme.colors.background.primary.copy(alpha = 0.75f),
                        TangemTheme.colors.background.primary.copy(alpha = 0.95f),
                        TangemTheme.colors.background.primary,
                    ),
                ),
            ),
    )
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
