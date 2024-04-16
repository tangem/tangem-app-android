package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.core.ui.test.TestTags
import io.github.kakaocup.compose.node.element.ComposeScreen

class WalletScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<WalletScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTags.WALLET_SCREEN) }
    )
