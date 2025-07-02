package com.tangem.features.welcome.impl.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.appbar.TopAppBarButton
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.fields.PinTextField
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.welcome.impl.ui.state.WelcomeUM

@Suppress("MagicNumber")
@Composable
internal fun AnimatedContentScope.WelcomeEnterAccessCode(
    state: WelcomeUM.EnterAccessCode,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column {
            TopAppBarButton(
                modifier = Modifier
                    .padding(12.dp),
                button = TopAppBarButtonUM.Back(onBackClicked = state.onBackClick),
                tint = TangemTheme.colors.icon.primary1,
            )

            SpacerH(68.dp)

            Text(
                modifier = Modifier
                    .animateEnterExit(
                        enter = slideInVertically(
                            tween(delayMillis = 300),
                            initialOffsetY = { it + 200 },
                        ) + fadeIn(tween(delayMillis = 300)),
                        exit = fadeOut(),
                    )
                    .align(Alignment.CenterHorizontally),
                text = "Enter Access Code",
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
            )

            SpacerH24()

            Box(
                modifier = Modifier
                    .animateEnterExit(
                        enter = slideInVertically(
                            tween(delayMillis = 300),
                            initialOffsetY = { it + 200 },
                        ) + fadeIn(tween(delayMillis = 300)),
                        exit = fadeOut(),
                    )
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PinTextField(
                    length = 6,
                    isPasswordVisual = true,
                    value = state.value,
                    onValueChange = state.onValueChange,
                )
            }
        }

        SecondaryButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding()
                .animateEnterExit(fadeIn(), fadeOut()),
            text = "Log in with biometric",
            onClick = state.onUnlockWithBiometricClick,
        )
    }
}