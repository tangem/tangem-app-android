package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class ReceiveAssetsBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ReceiveAssetsBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val showQrCodeButton: KNode = child {
        hasText(getResourceString(R.string.token_receive_show_qr_code_title))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onReceiveAssetsBottomSheet(function: ReceiveAssetsBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)