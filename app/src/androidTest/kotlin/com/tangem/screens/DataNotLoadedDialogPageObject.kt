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

class DataNotLoadedDialogPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<DataNotLoadedDialogPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(BaseDialogTestTags.TITLE)
        hasText(getResourceString(CommonUIR.string.action_buttons_service_loading_alert_title))
        useUnmergedTree = true
    }

    val text: KNode = child {
        hasTestTag(BaseDialogTestTags.TEXT)
        hasText(getResourceString(CommonUIR.string.action_buttons_service_loading_alert_message))
        useUnmergedTree = true
    }

    val okButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_ok))
    }
}

internal fun BaseTestCase.onDataNotLoadedDialog(function: DataNotLoadedDialogPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)