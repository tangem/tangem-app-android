package com.tangem.features.onramp.hottokens.portfolio.ui

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun OnrampAddToPortfolioBottomSheet(config: TangemBottomSheetConfig, content: @Composable () -> Unit) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = config,
        addBottomInsets = true,
        containerColor = TangemTheme.colors.background.tertiary,
        content = { content() },
    )
}