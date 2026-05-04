package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.AccountDetailsScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class AccountDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AccountDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val topAppBarBackButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
    }

    val manageTokensButton: KNode = child {
        hasTestTag(AccountDetailsScreenTestTags.MANAGE_TOKENS_BUTTON)
    }
}

internal fun BaseTestCase.onAccountDetails(function: AccountDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)