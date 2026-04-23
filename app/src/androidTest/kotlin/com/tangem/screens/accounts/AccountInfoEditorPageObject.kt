package com.tangem.screens.accounts

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.performClick
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.core.ui.test.accounts.AccountInfoEditScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class AccountInfoEditPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AccountInfoEditPageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(AccountInfoEditScreenTestTags.ACCOUNT_DETAILS_CONTAINER)
    }
    val accountNameField: KNode = child {
        hasTestTag(AccountInfoEditScreenTestTags.NAME_FIELD)
    }

    val accountCurrentIcon: KNode = child {
        hasTestTag(AccountInfoEditScreenTestTags.SELECTED_ICON)
    }

    val accountColorOption: KNode = child {
        hasTestTag(AccountInfoEditScreenTestTags.COLOR_OPTION)
    }

    val accountTypeOption: KNode = child {
        hasTestTag(AccountInfoEditScreenTestTags.TYPE_OPTION)
    }

    val saveAccountButton: KNode = child {
        hasTestTag(AccountInfoEditScreenTestTags.SAVE_ACCOUNT_BUTTON)
    }

    val crossButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
    }

    // @OptIn(ExperimentalTestApi::class)
    // fun pickRandomIcon() {
    //     val allIcons = semanticsProvider
    //         .onAllNodes(androidx.compose.ui.test.hasTestTag(AccountInfoEditScreenTestTags.TYPE_OPTION))
    //     val count = allIcons.fetchSemanticsNodes().size
    //
    //     require(count > 0) { "No icon items found" }
    //     allIcons[(Math.random() * count).toInt()].performClick()
    //
    // }

    // @OptIn(ExperimentalTestApi::class)
    // fun pickRandomColor() {
    //     val count = semanticsProvider
    //         .onNode(androidx.compose.ui.test.hasTestTag(AccountInfoEditScreenTestTags.COLORS_LIST))
    //         .fetchSemanticsNode()
    //         .children
    //         .size
    //
    //     require(count > 0) { "No color items found" }
    //
    //     colorsList.childAt<LazyListItemNode>((Math.random() * count).toInt()) {
    //         performClick()
    //     }
    // }

}

internal fun BaseTestCase.onAccountInfoEditorScreen(function: AccountInfoEditPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)