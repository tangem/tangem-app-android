package com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.OutlineTextFieldWithIcon
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui.state.MultiWalletAccessCodeUM
import kotlinx.coroutines.delay

@Composable
internal fun MultiWalletAccessCodeEnter(
    state: MultiWalletAccessCodeUM,
    reEnterAccessCodeState: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Text(
            text = if (reEnterAccessCodeState) {
                stringResource(R.string.onboarding_access_code_repeat_code_title)
            } else {
                stringResource(R.string.onboarding_access_code_intro_title)
            },
            style = TangemTheme.typography.subtitle1,
        )
        Text(
            text = if (reEnterAccessCodeState) {
                stringResource(R.string.onboarding_access_code_repeat_code_hint)
            } else {
                stringResource(R.string.onboarding_access_code_hint)
            },
            style = TangemTheme.typography.body1,
        )

        val focusRequester = remember { FocusRequester() }

        OutlineTextFieldWithIcon(
            modifier = modifier
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
            isError = state.codesNotMatchError || state.atLeast4CharError,
            visualTransformation = if (state.accessCodeHidden) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            caption = when {
                state.codesNotMatchError && reEnterAccessCodeState ->
                    stringResource(R.string.onboarding_access_codes_doesnt_match)
                state.atLeast4CharError && !reEnterAccessCodeState ->
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
        MultiWalletAccessCodeEnter(
            reEnterAccessCodeState = false,
            state = MultiWalletAccessCodeUM(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview2() {
    TangemThemePreview {
        MultiWalletAccessCodeEnter(
            reEnterAccessCodeState = true,
            state = MultiWalletAccessCodeUM(),
        )
    }
}