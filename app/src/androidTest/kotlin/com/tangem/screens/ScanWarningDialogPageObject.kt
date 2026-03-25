package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.tap.features.scanfails.ui.ScanFailsDialogTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class ScanWarningDialogPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ScanWarningDialogPageObject>(semanticsProvider = semanticsProvider) {

    val warningTitle: KNode = child {
        hasText(getResourceString(R.string.common_warning))
        useUnmergedTree = true
    }

    val warningMessage: KNode = child {
        hasText(getResourceString(R.string.alert_troubleshooting_scan_card_title))
        useUnmergedTree = true
    }

    val howToScanButton: KNode = child {
        hasTestTag(ScanFailsDialogTestTags.HOW_TO_SCAN_BUTTON)
        useUnmergedTree = true
    }

    val requestSupportButton: KNode = child {
        hasTestTag(ScanFailsDialogTestTags.REQUEST_SUPPORT_BUTTON)
        useUnmergedTree = true
    }

    val cancelButton: KNode = child {
        hasTestTag(ScanFailsDialogTestTags.CANCEL_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onScanWarningDialog(function: ScanWarningDialogPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)