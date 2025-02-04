package com.tangem.features.onboarding.v2.visa.impl.child.accesscode.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.components.OutlineTextFieldWithIcon
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemAnimations
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.ui.state.OnboardingVisaAccessCodeUM
import kotlinx.coroutines.delay

@Composable
internal fun OnboardingVisaAccessCode(state: OnboardingVisaAccessCodeUM, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        AnimatedContent(
            modifier = Modifier
                .padding(top = 48.dp)
                .weight(1f),
            targetState = state.step,
            transitionSpec = TangemAnimations.AnimatedContent
                .slide { initial, target -> initial.ordinal > target.ordinal },
        ) { step ->
            Content(
                state = state,
                reEnterAccessCodeState = step == OnboardingVisaAccessCodeUM.Step.ReEnter,
            )
        }

        SpacerH(20.dp)

        NavigationPrimaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .imePadding(),
            primaryButton = NavigationButton(
                textReference = when (state.step) {
                    OnboardingVisaAccessCodeUM.Step.Enter -> resourceReference(R.string.common_continue)
                    OnboardingVisaAccessCodeUM.Step.ReEnter ->
                        resourceReference(R.string.onboarding_create_wallet_button_create_wallet)
                },
                onClick = state.onContinue,
                iconRes = R.drawable.ic_tangem_24,
                showProgress = state.buttonLoading,
                isIconVisible = state.step == OnboardingVisaAccessCodeUM.Step.ReEnter,
            ),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun Content(
    state: OnboardingVisaAccessCodeUM,
    reEnterAccessCodeState: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 32.dp),
            text = if (reEnterAccessCodeState) {
                "Re-enter your access code" // TODO
            } else {
                "Create access code" // TODO
            },
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )

        SpacerH16()

        // TODO
        Text(
            modifier = Modifier
                .padding(horizontal = 32.dp),
            text = "The access code will be used manage your payment account and protect it from unauthorized access",
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )

        SpacerH32()

        val focusRequester = remember { FocusRequester() }

        OutlineTextFieldWithIcon(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            iconResId = if (state.accessCodeHidden) {
                R.drawable.ic_eye_outline_24
            } else {
                R.drawable.ic_eye_off_outline_24
            },
            iconColor = TangemTheme.colors.icon.primary1,
            onIconClick = state.onAccessCodeHideClick,
            value = if (reEnterAccessCodeState) {
                state.accessCodeSecond
            } else {
                state.accessCodeFirst
            },
            onValueChange = if (reEnterAccessCodeState) {
                state.onAccessCodeSecondChange
            } else {
                state.onAccessCodeFirstChange
            },
            label = stringResource(id = R.string.onboarding_wallet_info_title_third),
            isError = state.codesNotMatchError || state.atLeastMinCharsError,
            visualTransformation = if (state.accessCodeHidden) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            caption = when {
                state.codesNotMatchError && reEnterAccessCodeState ->
                    stringResource(R.string.onboarding_access_codes_doesnt_match)
                state.atLeastMinCharsError && !reEnterAccessCodeState ->
                    stringResource(R.string.onboarding_access_code_too_short)
                else -> null
            },
        )

        LaunchedEffect(Unit) {
            delay(timeMillis = 200)
            focusRequester.requestFocus()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        OnboardingVisaAccessCode(
            state = OnboardingVisaAccessCodeUM(),
        )
    }
}