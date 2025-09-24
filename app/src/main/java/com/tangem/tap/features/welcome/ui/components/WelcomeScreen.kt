package com.tangem.tap.features.welcome.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.tap.features.welcome.component.WelcomeComponent
import com.tangem.tap.features.welcome.component.impl.PreviewWelcomeComponent
import com.tangem.tap.features.welcome.ui.WelcomeScreenState
import com.tangem.tap.features.welcome.ui.model.WarningModel

@Composable
internal fun WelcomeScreen(state: WelcomeScreenState, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by rememberUpdatedState(newValue = state.error?.resolveReference())
    val warning by rememberUpdatedState(newValue = state.warning)

    BackHandler(onBack = state.onPopBack)

    Box(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .systemBarsPadding(),
    ) {
        WelcomeScreenContent(
            showUnlockProgress = state.isUnlockWithBiometricsProgressVisible,
            showScanCardProgress = state.isUnlockWithCardProgressVisible,
            onUnlockClick = state.onUnlockClick,
            onScanCardClick = state.onScanCardClick,
        )

        SnackbarHost(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            hostState = snackbarHostState,
        )
    }

    WarningDialog(warning)

    LaunchedEffect(errorMessage, state.onCloseError) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            state.onCloseError()
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_WelcomeScreen(
    @PreviewParameter(WelcomeComponentPreviewProvider::class) component: WelcomeComponent,
) {
    TangemThemePreview {
        component.Content(Modifier)
    }
}

private class WelcomeComponentPreviewProvider : PreviewParameterProvider<WelcomeComponent> {
    override val values: Sequence<WelcomeComponent>
        get() = sequenceOf(
            PreviewWelcomeComponent(),
            PreviewWelcomeComponent(
                initialState = WelcomeScreenState(
                    isUnlockWithBiometricsProgressVisible = true,
                    isUnlockWithCardProgressVisible = true,
                ),
            ),
            PreviewWelcomeComponent(
                initialState = WelcomeScreenState(
                    error = TextReference.Str(value = "Error"),
                ),
            ),
            PreviewWelcomeComponent(
                initialState = WelcomeScreenState(
                    warning = WarningModel.KeyInvalidatedWarning(onDismiss = {}),
                ),
            ),
        )
}
// endregion Preview