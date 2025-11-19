package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.QrCodeScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class ScanQrPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ScanQrPageObject>(semanticsProvider = semanticsProvider) {

    val backTopAppBarButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val sendTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.common_send))
        useUnmergedTree = true
    }

    val flashlightButton: KNode = child {
        hasTestTag(QrCodeScreenTestTags.FLASHLIGHT_BUTTON)
        useUnmergedTree = true
    }

    val galleryButton: KNode = child {
        hasTestTag(QrCodeScreenTestTags.GALLERY_BUTTON)
        useUnmergedTree = true
    }

    fun scanText(tokenName: String): KNode = child {
        hasText(getResourceString(R.string.send_qrcode_scan_info, tokenName))
        useUnmergedTree = true
    }

    val pasteFromClipboardButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.wallet_connect_paste_from_clipboard))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onScanQrScreen(function: ScanQrPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)