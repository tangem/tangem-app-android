package com.tangem.feature.onboarding.presentation.wallet2.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.presentation.wallet2.model.ShowPassphraseInfoBottomSheetContent

@Composable
fun PassphraseInfoBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.primary,
    ) { content: ShowPassphraseInfoBottomSheetContent ->
        PassphraseInfoBottomSheetContent(content)
    }
}

@Composable
fun PassphraseInfoBottomSheetContent(content: ShowPassphraseInfoBottomSheetContent) {
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
            text = stringResourceSafe(id = R.string.common_passphrase),
            modifier = Modifier
                .padding(top = TangemTheme.dimens.size40)
                .align(Alignment.CenterHorizontally),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.h2,
        )

        Text(
            text = stringResourceSafe(id = R.string.onboarding_bottom_sheet_passphrase_description),
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
            text = stringResourceSafe(id = R.string.common_ok),
            onClick = content.onOkClick,
        )
    }
}

@Preview
@Composable
private fun PassphraseInfoBottomSheetContentPreview() {
    TangemThemePreview {
        PassphraseInfoBottomSheetContent(ShowPassphraseInfoBottomSheetContent { })
    }
}