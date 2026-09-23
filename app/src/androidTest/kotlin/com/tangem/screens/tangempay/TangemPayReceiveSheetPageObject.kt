package com.tangem.screens.tangempay

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.intercept.operation.ComposeOperationType
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

/**
 * Pay-specific multi-token receive sheet reached from 'Add funds' → 'From another wallet or exchange'
 * → network. Its address view and its QR view never compose together, so [address], [copyButton] and
 * [shareButton] address whichever of the two is on screen.
 */
class TangemPayReceiveSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayReceiveSheetPageObject>(semanticsProvider = semanticsProvider) {

    val address: KNode = child {
        hasTestTag(TangemPayTestTags.RECEIVE_ADDRESS)
        useUnmergedTree = true
    }

    val copyButton: KNode = child {
        hasTestTag(TangemPayTestTags.RECEIVE_COPY_BUTTON)
        useUnmergedTree = true
    }

    val shareButton: KNode = child {
        hasTestTag(TangemPayTestTags.RECEIVE_SHARE_BUTTON)
        useUnmergedTree = true
    }

    val showQrCodeButton: KNode = child {
        hasTestTag(TangemPayTestTags.RECEIVE_SHOW_QR_BUTTON)
        useUnmergedTree = true
    }

    val qrCode: KNode = child {
        hasTestTag(TangemPayTestTags.RECEIVE_QR_CODE)
        useUnmergedTree = true
    }

    /** "Send only {tokens} on the {network} network" headline of the warning banner. */
    fun warningTitle(tokensLabel: String, networkName: String): KNode = child {
        hasText(getResourceString(CoreResR.string.receive_bottom_sheet_warning_title, tokensLabel, networkName))
        useUnmergedTree = true
    }

    val warningDescription: KNode = child {
        hasText(getResourceString(CoreResR.string.receive_bottom_sheet_warning_message_description))
        useUnmergedTree = true
    }

    /** "{tokens} on {network} network" heading above the deposit address. */
    fun tokensOnNetworkHeading(tokensLabel: String, networkName: String): KNode = child {
        hasText(
            getResourceString(
                CoreResR.string.receive_bottom_sheet_warning_message_compact,
                tokensLabel,
                networkName,
            ),
        )
        useUnmergedTree = true
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

internal fun BaseTestCase.onTangemPayReceiveSheet(function: TangemPayReceiveSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)