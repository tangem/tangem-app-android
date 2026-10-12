package com.tangem.features.tangempay.withdraw.bank.recipients.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.search.TangemSearch

@Composable
internal fun BankWithdrawSearchFooter(state: TangemSearch.State, modifier: Modifier = Modifier) {
    val navigationBarInset = WindowInsets.navigationBars
        .exclude(WindowInsets.ime)
        .asPaddingValues()
        .calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime),
        contentAlignment = Alignment.BottomCenter,
    ) {
        TangemFade(
            position = TangemFade.Position.Bottom,
            variant = TangemFade.Variant.Hard,
            modifier = Modifier
                .fillMaxWidth()
                .height(FadeHeight + navigationBarInset),
        )
        TangemSearch(
            state = state,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp + navigationBarInset),
        )
    }
}

private val FadeHeight = 112.dp