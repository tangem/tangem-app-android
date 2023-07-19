package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.PrimaryButtonIconStart
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.WalletBottomSheetConfig

/**
 * Wallet bottom sheet with detail notification information
 *
 * @param config component config
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WalletBottomSheet(config: WalletBottomSheetConfig) {
    ModalBottomSheet(
        onDismissRequest = config.onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = TangemTheme.colors.background.primary,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        BottomSheetContent(config = config.content)
    }
}

@Composable
private fun BottomSheetContent(config: WalletBottomSheetConfig.BottomSheetContentConfig) {
    Column(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .padding(top = TangemTheme.dimens.spacing40, bottom = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing40),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconTint = config.tint
        if (iconTint != null) {
            Icon(
                painter = painterResource(id = config.iconResId),
                contentDescription = null,
                modifier = Modifier.size(size = TangemTheme.dimens.size48),
                tint = iconTint,
            )
        } else {
            Image(
                painter = painterResource(id = config.iconResId),
                contentDescription = null,
                modifier = Modifier.size(size = TangemTheme.dimens.size48),
            )
        }

        Text(
            text = config.title.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.h2,
        )

        Text(
            text = config.subtitle.resolveReference(),
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.body2,
        )

        Column(verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing10)) {
            val buttonModifier = Modifier.fillMaxWidth()
            PrimaryButton(config = config.primaryButtonConfig, modifier = buttonModifier)

            if (config.secondaryButtonConfig != null) {
                SecondaryButton(config = config.secondaryButtonConfig, modifier = buttonModifier)
            }
        }
    }
}

@Composable
private fun PrimaryButton(
    config: WalletBottomSheetConfig.BottomSheetContentConfig.ButtonConfig,
    modifier: Modifier = Modifier,
) {
    if (config.iconResId == null) {
        PrimaryButton(
            text = config.text,
            onClick = config.onClick,
            modifier = modifier,
        )
    } else {
        PrimaryButtonIconStart(
            text = config.text,
            iconResId = config.iconResId,
            onClick = config.onClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun SecondaryButton(
    config: WalletBottomSheetConfig.BottomSheetContentConfig.ButtonConfig,
    modifier: Modifier = Modifier,
) {
    if (config.iconResId == null) {
        SecondaryButton(
            text = config.text,
            onClick = config.onClick,
            modifier = modifier,
        )
    } else {
        SecondaryButtonIconStart(
            text = config.text,
            iconResId = config.iconResId,
            onClick = config.onClick,
            modifier = modifier,
        )
    }
}

@Preview
@Composable
private fun WalletBottomSheetContent_Light(
    @PreviewParameter(WalletBottomSheetConfigProvider::class)
    config: WalletBottomSheetConfig,
) {
    TangemTheme(isDark = false) {
        // Use preview of content because ModalBottomSheet isn't supported in Preview mode
        BottomSheetContent(config = config.content)
    }
}

@Preview
@Composable
private fun WalletBottomSheetContent_Dark(
    @PreviewParameter(WalletBottomSheetConfigProvider::class)
    config: WalletBottomSheetConfig,
) {
    TangemTheme(isDark = false) {
        // Use preview of content because ModalBottomSheet isn't supported in Preview mode
        BottomSheetContent(config = config.content)
    }
}

private class WalletBottomSheetConfigProvider : CollectionPreviewParameterProvider<WalletBottomSheetConfig>(
    collection = listOf(WalletPreviewData.bottomSheet),
)