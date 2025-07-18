package com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.extensions.stringResourceSafe
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
                stringResourceSafe(R.string.onboarding_access_code_repeat_code_title)
            } else {
                stringResourceSafe(R.string.onboarding_access_code_intro_title)
            },
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = if (reEnterAccessCodeState) {
                stringResourceSafe(R.string.onboarding_access_code_repeat_code_hint)
            } else {
                stringResourceSafe(R.string.onboarding_access_code_hint)
            },
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
        )

        val focusRequester = remember { FocusRequester() }

        OutlineTextField(
            modifier = modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
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
            label = stringResourceSafe(id = R.string.onboarding_wallet_info_title_third),
            isError = state.codesNotMatchError,
            visualTransformation = PasswordVisualTransformation(),
            caption = when {
                state.codesNotMatchError && reEnterAccessCodeState ->
                    stringResourceSafe(R.string.onboarding_access_codes_doesnt_match)
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