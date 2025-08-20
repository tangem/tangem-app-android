package com.tangem.features.hotwallet.accesscoderequest.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.fields.PinTextColor
import com.tangem.core.ui.components.fields.PinTextField
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.accesscoderequest.entity.HotAccessCodeRequestUM
import com.tangem.features.hotwallet.impl.R

@Suppress("MagicNumber", "LongMethod")
@Composable
internal fun HotAccessCodeRequestFullScreenContent(state: HotAccessCodeRequestUM, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = modifier,
        visible = state.isShown,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.primary),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TangemTopAppBar(
                modifier = Modifier
                    .statusBarsPadding(),
                startButton = TopAppBarButtonUM.Back(state.onDismiss),
            )

            SpacerH(68.dp)

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.animateEnterExit(
                        enter = slideInVertically(
                            tween(),
                            initialOffsetY = { it + 200 },
                        ) + fadeIn(tween()),
                        exit = slideOutVertically(tween(300)) { it - 200 } + fadeOut(tween()),
                    ),
                    text = stringResourceSafe(R.string.access_code_check_title),
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                )

                SpacerH24()

                PinTextField(
                    modifier = Modifier.animateEnterExit(
                        enter = slideInVertically(
                            tween(),
                            initialOffsetY = { it + 200 },
                        ) + fadeIn(tween()),
                        exit = slideOutVertically(tween(300)) { it - 200 } + fadeOut(tween()),
                    ),
                    length = 6,
                    isPasswordVisual = true,
                    value = state.accessCode,
                    pinTextColor = state.accessCodeColor,
                    onValueChange = state.onAccessCodeChange,
                )

                SpacerH(20.dp)

                AnimatedVisibility(
                    modifier = Modifier.animateEnterExit(
                        enter = slideInVertically(
                            tween(),
                            initialOffsetY = { it + 200 },
                        ) + fadeIn(tween()),
                        exit = slideOutVertically(tween(300)) { it - 200 } + fadeOut(tween()),
                    ),
                    visible = state.wrongAccessCodeText != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    val wrongAccessCodeText =
                        state.wrongAccessCodeText ?: return@AnimatedVisibility

                    Text(
                        text = wrongAccessCodeText.resolveReference(),
                        textAlign = TextAlign.Center,
                        style = TangemTheme.typography.caption2.copy(
                            lineBreak = LineBreak.Heading,
                        ),
                        color = TangemTheme.colors.text.warning,
                    )
                }
            }

            if (state.useBiometricVisible) {
                SecondaryButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    text = "Use biometric",
                    onClick = state.useBiometricClick,
                )
            }
        }
    }

    val hapticManager = LocalHapticManager.current

    LaunchedEffect(state.accessCodeColor) {
        when (state.accessCodeColor) {
            PinTextColor.WrongCode -> {
                hapticManager.perform(TangemHapticEffect.View.Reject)
            }
            PinTextColor.Success -> {
                hapticManager.perform(TangemHapticEffect.View.Confirm)
            }
            else -> Unit
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        Box(
            Modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.secondary),
        ) {
            var isShown by remember { mutableStateOf(true) }

            HotAccessCodeRequestFullScreenContent(
                state = HotAccessCodeRequestUM(
                    isShown = isShown,
                    wrongAccessCodeText = stringReference("Wrong access code"),
                ),
                modifier = Modifier,
            )

            Button(onClick = { isShown = !isShown }) {
                Text("TOggle")
            }
        }
    }
}