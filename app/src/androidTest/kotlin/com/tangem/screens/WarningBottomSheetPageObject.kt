package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class WarningBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<WarningBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val icon: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.ICON)
        useUnmergedTree = true
    }

    fun title(title: String): KNode = child {
        hasTestTag(WarningBottomSheetTestTags.TITLE)
        hasText(title)
        useUnmergedTree = true
    }

    fun message(message: String): KNode = child {
        hasTestTag(WarningBottomSheetTestTags.MESSAGE)
        hasText(message)
        useUnmergedTree = true
    }

    val okGotItButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_SECONDARY)
        hasText(getResourceString(R.string.warning_button_ok))
    }

    val gotItButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_PRIMARY)
        hasText(getResourceString(R.string.common_got_it))
    }

    val cancelButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_PRIMARY)
        hasText(getResourceString(R.string.common_cancel))
    }

    val connectAnywayButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_SECONDARY)
        hasText(getResourceString(R.string.wc_alert_connect_anyway))
    }
}

internal fun BaseTestCase.onWarningBottomSheet(function: WarningBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)