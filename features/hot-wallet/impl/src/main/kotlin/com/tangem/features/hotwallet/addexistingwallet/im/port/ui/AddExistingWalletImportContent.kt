package com.tangem.features.hotwallet.addexistingwallet.im.port.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.addexistingwallet.im.port.entity.AddExistingWalletImportUM
import com.tangem.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddExistingWalletImportContent(state: AddExistingWalletImportUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TangemTopAppBar(
            modifier = Modifier
                .statusBarsPadding(),
            startButton = TopAppBarButtonUM.Back(state.onBackClick),
            title = stringResourceSafe(R.string.wallet_import_title),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAddExistingWalletImportContent() {
    TangemThemePreview {
        AddExistingWalletImportContent(
            state = AddExistingWalletImportUM(
                onBackClick = {},
            ),
        )
    }
}