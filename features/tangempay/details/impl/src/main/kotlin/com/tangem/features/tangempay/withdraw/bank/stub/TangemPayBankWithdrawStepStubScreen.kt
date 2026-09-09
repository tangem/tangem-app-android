package com.tangem.features.tangempay.withdraw.bank.stub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM

@Composable
internal fun TangemPayBankWithdrawStepStubScreen(
    header: BankWithdrawHeaderUM,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        TangemTopNavigation(
            title = header.title,
            subtitle = header.subtitle,
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            blurBackground = false,
            onBack = onBackClick,
        )
    }
}