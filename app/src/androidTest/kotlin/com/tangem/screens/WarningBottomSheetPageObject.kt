package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class WarningBottomSheetPageObject(private val semanticsProvider: SemanticsNodeInteractionsProvider) :
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

    val finishWalletActivationTitle: KNode = title(getResourceString(CoreResR.string.hw_activation_need_title))

    val finishWalletActivationLaterButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_SECONDARY)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.common_later)))
        useUnmergedTree = true
    }

    // Raw Compose on purpose: a Kakao check of an absent sheet waits out Kaspresso's 10 s flaky-safety retry.
    fun isFinishWalletActivationShown(): Boolean = semanticsProvider.onAllNodes(
        matcher = withTestTag(WarningBottomSheetTestTags.TITLE) and
            withText(getResourceString(CoreResR.string.hw_activation_need_title)),
        useUnmergedTree = true,
    ).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
}

internal fun BaseTestCase.onWarningBottomSheet(function: WarningBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)