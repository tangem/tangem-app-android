package com.tangem.managetokens.presentation.addcustomtoken.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.common.ui.components.SimpleSelectionBlock
import com.tangem.managetokens.presentation.addcustomtoken.state.ChooseDerivationState
import com.tangem.managetokens.presentation.addcustomtoken.state.previewdata.ChooseDerivationPreviewData

@Composable
internal fun ChooseDerivationScreen(state: ChooseDerivationState, modifier: Modifier = Modifier) {
    if (state.enterCustomDerivationState != null) {
        CustomDerivationDialog(state.enterCustomDerivationState)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors.background.tertiary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(
                top = TangemTheme.dimens.spacing10,
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing18,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = TangemTheme.dimens.size44)
                .padding(bottom = TangemTheme.dimens.spacing12),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.primary1,
                modifier = Modifier
                    .clickable { state.onCloseChoosingDerivationClick() },
            )
            Text(
                text = stringResource(id = R.string.custom_token_derivation_path),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing24)
                    .align(Alignment.Center),
            )
        }
        DerivationsList(state)
    }
}

@Composable
private fun DerivationsList(state: ChooseDerivationState) {
    LazyColumn(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16),
        contentPadding = PaddingValues(vertical = TangemTheme.dimens.spacing12),
    ) {
        item {
            SimpleSelectionBlock(
                title = stringResource(id = R.string.custom_token_custom_derivation),
                subtitle = stringResource(id = R.string.custom_token_custom_derivation),
                onClick = state.onEnterCustomDerivation,
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
            )
        }
        items(state.derivations.count()) { index ->
            val derivationItem = state.derivations[index]
            SimpleSelectionBlock(
                title = derivationItem.networkName,
                subtitle = derivationItem.path,
                onClick = { derivationItem.onDerivationSelected(derivationItem) },
                modifier = Modifier.roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = state.derivations.lastIndex,
                    addDefaultPadding = false,
                ),
                roundedCorners = false,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ChooseDerivationScreen() {
    TangemThemePreview {
        ChooseDerivationScreen(state = ChooseDerivationPreviewData.state)
    }
}