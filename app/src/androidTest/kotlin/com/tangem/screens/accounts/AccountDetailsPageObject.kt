package com.tangem.screens.accounts

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.accounts.AccountDetailsScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class AccountDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AccountDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(AccountDetailsScreenTestTags.ACCOUNT_DETAILS_CONTAINER)
    }

    val topAppBarBackButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
    }

    val manageTokensButton: KNode = child {
        hasTestTag(AccountDetailsScreenTestTags.MANAGE_TOKENS_BUTTON)
    }

    val archiveAccountButton: KNode = child {
        hasTestTag(AccountDetailsScreenTestTags.ARCHIVE_ACCOUNT_BUTTON)
    }

    val editAccountButton: KNode = child {
        hasTestTag(AccountDetailsScreenTestTags.EDIT_ACCOUNT_BUTTON)
    }

}

internal fun BaseTestCase.onAccountDetailsScreen(function: AccountDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)