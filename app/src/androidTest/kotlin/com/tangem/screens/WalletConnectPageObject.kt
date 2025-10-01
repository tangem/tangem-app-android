package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.WalletConnectScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class WalletConnectPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<WalletConnectPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(R.string.wc_connections))
        useUnmergedTree = true
    }

    val moreButton: KNode = child {
        hasTestTag(WalletConnectScreenTestTags.MORE_BUTTON)
        useUnmergedTree = true
    }

    val walletName: KNode = child {
        hasTestTag(WalletConnectScreenTestTags.WALLET_NAME)
        useUnmergedTree = true
    }

    val appIcon: KNode = child {
        hasTestTag(WalletConnectScreenTestTags.APP_ICON)
        useUnmergedTree = true
    }

    val appName: KNode = child {
        hasTestTag(WalletConnectScreenTestTags.APP_NAME)
        useUnmergedTree = true
    }

    val approveIcon: KNode = child {
        hasTestTag(WalletConnectScreenTestTags.APPROVE_ICON)
        useUnmergedTree = true
    }

    val appUrl: KNode = child {
        hasTestTag(WalletConnectScreenTestTags.APP_URL)
        useUnmergedTree = true
    }

    val newConnectionButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.wc_new_connection))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onWalletConnectScreen(function: WalletConnectPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)