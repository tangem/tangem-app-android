package com.tangem.screens

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.TokenReceiveQrCodeBottomSheetTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.intercept.operation.ComposeOperationType
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
        hasText(getResourceString(R.string.common_copy))
        hasClickAction()
    }

    val shareButton: KNode = child {
        hasText(getResourceString(R.string.common_share))
        hasClickAction()
    }

    /** Captures the QR code node as a bitmap via the Kakao node delegate (no raw composeTestRule access). */
    fun captureQrCodeBitmap(): Bitmap {
        lateinit var bitmap: Bitmap
        qrCode.delegate.perform(QrCodeAction.CAPTURE) {
            bitmap = captureToImage().asAndroidBitmap()
        }
        return bitmap
    }

    private enum class QrCodeAction : ComposeOperationType { CAPTURE }
}

internal fun BaseTestCase.onTokenReceiveQrCodeBottomSheet(function: TokenReceiveQrCodeBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)