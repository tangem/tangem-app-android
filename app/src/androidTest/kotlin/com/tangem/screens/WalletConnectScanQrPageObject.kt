package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class WalletConnectScanQrPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<WalletConnectScanQrPageObject>(semanticsProvider = semanticsProvider) {

    val pasteFromClipboardButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.wallet_connect_paste_from_clipboard))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onWalletConnectScanQrScreen(function: WalletConnectScanQrPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)