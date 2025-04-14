package com.tangem.features.onboarding.v2.twin.impl.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH16
import com.tangem.common.ui.bottomsheet.receive.TokenReceiveBottomSheet
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemAnimations
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.twin.impl.ui.state.OnboardingTwinUM

@Suppress("UnusedPrivateMember", "MagicNumber")
@Composable
internal fun OnboardingTwin(state: OnboardingTwinUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        TwinWalletArtworks(
            modifier = Modifier
                .padding(top = 64.dp)
                .padding(horizontal = 24.dp)
                .weight(.48f)
                .fillMaxWidth(),
            state = state.artwork,
            balance = (state as? OnboardingTwinUM.TopUp)?.balance ?: "",
            isRefreshing = state.isLoading,
            onRefreshBalanceClick = {
                (state as? OnboardingTwinUM.TopUp)?.onRefreshClick()
            },
        )

        AnimatedContent(
            modifier = Modifier.weight(.52f),
            targetState = state,
            transitionSpec = TangemAnimations.AnimatedContent
                .slide { initial, target -> target.stepIndex > initial.stepIndex },
            contentKey = { it::class },
        ) { st ->
            when (st) {
                is OnboardingTwinUM.ResetWarning -> ResetWarning(st)
                is OnboardingTwinUM.ScanCard -> ScanCard(st)
                is OnboardingTwinUM.TopUp -> TopUp(st)
                is OnboardingTwinUM.Welcome -> Welcome(st)
                OnboardingTwinUM.TopUpPrepare -> {}
            }
        }
    }

    if (state is OnboardingTwinUM.TopUp) {
        TokenReceiveBottomSheet(config = state.bottomSheetConfig)
    }
}

@Suppress("LongMethod")
@Composable
private fun ResetWarning(state: OnboardingTwinUM.ResetWarning, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResourceSafe(R.string.common_warning),
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.h2,
            )

            SpacerH16()

            Text(
                text = stringResourceSafe(R.string.twins_recreate_warning),
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.body1,
            )

            SpacerH16()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { state.onAcceptClick(!state.acceptToggle) },
                    ),
            ) {
                IconToggleButton(checked = state.acceptToggle, onCheckedChange = state.onAcceptClick) {
                    AnimatedContent(targetState = state.acceptToggle, label = "Update checked state") { checked ->
                        Icon(
                            painter = painterResource(
                                if (checked) {
                                    R.drawable.ic_accepted_20
                                } else {
                                    R.drawable.ic_unticked_20
                                },
                            ),
                            contentDescription = null,
                            tint = if (checked) {
                                TangemTheme.colors.control.checked
                            } else {
                                TangemTheme.colors.icon.secondary
                            },
                        )
                    }
                }

                Text(
                    text = stringResourceSafe(R.string.common_understand),
                    color = TangemTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                    style = TangemTheme.typography.body1,
                )
            }
        }

        PrimaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.common_continue),
            enabled = state.acceptToggle,
            onClick = state.onContinueClick,
        )
    }
}

@Composable
private fun TopUp(state: OnboardingTwinUM.TopUp, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResourceSafe(R.string.onboarding_topup_title),
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.h2,
            )

            SpacerH16()

            Text(
                text = stringResourceSafe(R.string.onboarding_top_up_body),
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.body1,
            )
        }

        PrimaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.onboarding_top_up_button_but_crypto),
            onClick = state.onBuyCryptoClick,
        )

        SecondaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.onboarding_top_up_button_show_wallet_address),
            onClick = state.onShowAddressClick,
        )
    }
}

@Composable
private fun ScanCard(state: OnboardingTwinUM.ScanCard, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResourceSafe(R.string.twins_recreate_title_format, state.cardNumber),
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.h2,
            )

            SpacerH16()

            Text(
                text = stringResourceSafe(R.string.onboarding_twins_interrupt_warning),
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.body1,
            )
        }

        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.onboarding_button_backup_card_format, state.cardNumber),
            showProgress = state.isLoading,
            iconResId = R.drawable.ic_tangem_24,
            onClick = state.onScanClick,
        )
    }
}

@Composable
private fun Welcome(state: OnboardingTwinUM.Welcome, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResourceSafe(R.string.twins_onboarding_subtitle),
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.h2,
            )

            SpacerH16()

            Text(
                text = stringResourceSafe(R.string.twins_onboarding_description_format, state.pairCardNumber),
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.body1,
            )
        }

        PrimaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.common_continue),
            onClick = state.onContinueClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTopUp() {
    TangemThemePreview {
        OnboardingTwin(OnboardingTwinUM.TopUp())
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewWelcome() {
    TangemThemePreview {
        OnboardingTwin(OnboardingTwinUM.Welcome())
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewScanCard() {
    TangemThemePreview {
        OnboardingTwin(OnboardingTwinUM.ScanCard())
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewResetWarning() {
    TangemThemePreview {
        var toggle by remember { mutableStateOf(false) }

        OnboardingTwin(
            OnboardingTwinUM.ResetWarning(
                acceptToggle = toggle,
                onAcceptClick = { toggle = it },
            ),
        )
    }
}