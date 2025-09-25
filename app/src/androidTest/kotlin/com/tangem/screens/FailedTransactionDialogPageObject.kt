package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.BaseDialogTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.common.ui.R as CommonUIR

class FailedTransactionDialogPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<FailedTransactionDialogPageObject>(semanticsProvider = semanticsProvider) {

    val dialogContainer: KNode = child {
        hasTestTag(BaseDialogTestTags.CONTAINER)
    }

    val title: KNode = child {
        hasTestTag(BaseDialogTestTags.TITLE)
        hasText(getResourceString(CommonUIR.string.send_alert_transaction_failed_title))
        useUnmergedTree = true
    }

    val text: KNode = child {
        hasTestTag(BaseDialogTestTags.TEXT)
        useUnmergedTree = true
    }

    val cancelButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_cancel))
    }

    val supportButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_support))
    }
}

internal fun BaseTestCase.onFailedTransactionDialog(function: FailedTransactionDialogPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)