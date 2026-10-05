package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.BaseDialogTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.core.res.R as CoreResR

class AccountLimitDialogPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AccountLimitDialogPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(BaseDialogTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.account_add_limit_dialog_title))
        useUnmergedTree = true
    }

    val message: KNode = child {
        hasTestTag(BaseDialogTestTags.TEXT)
        hasText(getResourceString(CoreResR.string.account_add_limit_dialog_description).format("20"))
        useUnmergedTree = true
    }

    val gotItButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(CoreResR.string.common_got_it))
    }
}

internal fun BaseTestCase.onAccountLimitDialog(function: AccountLimitDialogPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)