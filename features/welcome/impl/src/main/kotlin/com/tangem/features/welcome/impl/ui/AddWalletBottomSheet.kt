package com.tangem.features.welcome.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.welcome.impl.ui.state.AddWalletBottomSheetContentUM

@Composable
fun AddWalletBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<AddWalletBottomSheetContentUM>(
        config = config,
        titleText = TextReference.Str("Add Wallet"),
        containerColor = TangemTheme.colors.background.tertiary,
        content = { Content(it) },
    )
}

@Composable
private fun Content(content: AddWalletBottomSheetContentUM) {
    Column(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        InputRowDefault(
            text = TextReference.Str("Create New Wallet"),
            modifier = Modifier
                .roundedShapeItemDecoration(
                    currentIndex = 0,
                    lastIndex = 3,
                    addDefaultPadding = false,
                )
                .background(TangemTheme.colors.background.action)
                .clickable { content.onOptionClick(AddWalletBottomSheetContentUM.Option.Create) },
        )
        InputRowDefault(
            text = TextReference.Str("Add Existing Wallet"),
            modifier = Modifier
                .roundedShapeItemDecoration(
                    currentIndex = 1,
                    lastIndex = 2,
                    addDefaultPadding = false,
                )
                .background(TangemTheme.colors.background.action)
                .clickable { content.onOptionClick(AddWalletBottomSheetContentUM.Option.Add) },
        )
        InputRowDefault(
            text = TextReference.Str("Buy Tangem Wallet"),
            modifier = Modifier
                .roundedShapeItemDecoration(
                    currentIndex = 2,
                    lastIndex = 2,
                    addDefaultPadding = false,
                )
                .background(TangemTheme.colors.background.action)
                .clickable { content.onOptionClick(AddWalletBottomSheetContentUM.Option.Buy) },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityScoreBottomSheetPreview() {
    TangemThemePreview {
        AddWalletBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = AddWalletBottomSheetContentUM(),
            ),
        )
    }
}