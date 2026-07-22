package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

// Scoped to the bottom-sheet container: the swap token-search screen stays behind the sheet and
// would otherwise make text matches (wallet tabs, market 'Ethereum' item) ambiguous.
class AddToPortfolioPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AddToPortfolioPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BaseBottomSheetTestTags.CONTAINER) },
    ) {

    fun walletName(walletName: String): KNode = child {
        hasText(walletName)
        useUnmergedTree = true
    }

    /** The wallet row on the 'Add token' screen (label + selected value); tap to open the wallet selector. */
    val selectedWalletRow: KNode = child {
        hasText(getResourceString(R.string.wc_common_wallet))
        hasClickAction()
    }

    /** A wallet item inside the portfolio selector screen. */
    fun walletOption(walletName: String): KNode = child {
        hasText(walletName)
        hasClickAction()
    }

    /** The network row on the 'Add token' screen; tap to open the 'Choose network' selector. */
    val networkRow: KNode = child {
        hasText(getResourceString(R.string.wc_common_network))
        hasClickAction()
    }

    /** A network item inside the 'Choose network' selector. */
    fun networkOption(networkName: String): KNode = child {
        hasText(networkName)
        hasClickAction()
    }

    val confirmButton: KNode = child {
        hasText(getResourceString(R.string.common_confirm))
        hasClickAction()
    }
}

internal fun BaseTestCase.onAddToPortfolioScreen(function: AddToPortfolioPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)