package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.TokenReceiveQrCodeBottomSheetTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TokenReceiveQrCodeBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TokenReceiveQrCodeBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val closeButton: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasTestTag(TokenReceiveQrCodeBottomSheetTestTags.TITLE)
        useUnmergedTree = true
    }

    val qrCode: KNode = child {
        hasTestTag(TokenReceiveQrCodeBottomSheetTestTags.QR_CODE)
        useUnmergedTree = true
    }

    val addressTitle: KNode = child {
        hasText(getResourceString(R.string.wc_common_address))
        useUnmergedTree = true
    }

    val address: KNode = child {
        hasTestTag(TokenReceiveQrCodeBottomSheetTestTags.ADDRESS)
        useUnmergedTree = true
    }

    val copyButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_copy))
        useUnmergedTree = true
    }

    val shareButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_share))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTokenReceiveQrCodeBottomSheet(function: TokenReceiveQrCodeBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)