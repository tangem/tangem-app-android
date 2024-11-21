package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R

@Composable
fun PassphraseInfoBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.primary,
    ) { _: TangemBottomSheetConfigContent.Empty ->
        PassphraseInfoBottomSheetContent(config.onDismissRequest)
    }
}

@Composable
fun PassphraseInfoBottomSheetContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.primary)
            .fillMaxWidth(),
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = TangemTheme.dimens.size40)
                .size(TangemTheme.dimens.size48),
            painter = painterResource(id = R.drawable.ic_information_24),
            tint = TangemTheme.colors.icon.accent,
            contentDescription = null,
        )

        Text(
            text = stringResource(id = R.string.common_passphrase),
            modifier = Modifier
                .padding(top = TangemTheme.dimens.size40)
                .align(Alignment.CenterHorizontally),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.h2,
        )

        Text(
            text = stringResource(id = R.string.onboarding_bottom_sheet_passphrase_description),
            modifier = Modifier
                .padding(top = TangemTheme.dimens.size16)
                .padding(horizontal = TangemTheme.dimens.size24)
                .align(Alignment.CenterHorizontally),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.body2,
            textAlign = TextAlign.Center,
        )

        PrimaryButton(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.size16)
                .padding(top = TangemTheme.dimens.size40)
                .padding(bottom = TangemTheme.dimens.size32)
                .fillMaxWidth(),
            text = stringResource(id = R.string.common_ok),
            onClick = onDismiss,
        )
    }
}

@Preview
@Composable
private fun PassphraseInfoBottomSheetContentPreview() {
    TangemThemePreview {
        PassphraseInfoBottomSheetContent({ })
    }
}
