package com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui.state.MultiWalletAccessCodeUM

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

        OutlineTextField(
            modifier = modifier.fillMaxWidth(),
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
            isError = state.codesNotMatchError,
            caption = if (state.codesNotMatchError && reEnterAccessCodeState) {
                stringResource(R.string.onboarding_access_codes_doesnt_match)
            } else {
                null
            },
        )
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