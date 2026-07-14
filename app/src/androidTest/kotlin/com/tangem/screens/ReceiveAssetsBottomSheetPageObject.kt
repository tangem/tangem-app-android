package com.tangem.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.TokenReceiveAssetsBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class ReceiveAssetsBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ReceiveAssetsBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    /** 'Show QR code' button of the address at [index]; both pager cards stay composed, so match by position. */
    fun showQrCodeButton(index: Int = 0): KNode = child {
        hasText(getResourceString(R.string.token_receive_show_qr_code_title))
        hasPosition(index)
        useUnmergedTree = true
    }

    val addressesPager: KNode = child {
        hasTestTag(TokenReceiveAssetsBottomSheetTestTags.ADDRESSES_PAGER)
        useUnmergedTree = true
    }

    /** Pages the addresses carousel to the address of the given [index]. */
    @OptIn(ExperimentalTestApi::class)
    fun scrollToAddress(index: Int) {
        addressesPager { performScrollToIndex(index) }
    }
}

internal fun BaseTestCase.onReceiveAssetsBottomSheet(function: ReceiveAssetsBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)