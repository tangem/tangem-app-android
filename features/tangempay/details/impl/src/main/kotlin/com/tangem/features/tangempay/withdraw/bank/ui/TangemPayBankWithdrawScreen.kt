package com.tangem.features.tangempay.withdraw.bank.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_20
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry

@Composable
internal fun TangemPayBankWithdrawScreen(title: TextReference, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        TangemTopBar(
            modifier = Modifier.statusBarsPadding(),
            title = title,
            startContent = {
                TangemButton(
                    iconStart = TangemIconUM.Icon(Icons.ic_chevron_left_20),
                    onClick = onBackClick,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayBankWithdrawScreenPreview() {
    TangemThemePreviewRedesign {
        TangemPayBankWithdrawScreen(
            title = BankWithdrawEntry.NewPersonal.toScreenTitle(),
            onBackClick = {},
        )
    }
}