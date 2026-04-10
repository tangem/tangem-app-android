package com.tangem.screens.accounts

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.accounts.AccountDetailsScreenTestTags
import com.tangem.core.ui.test.SendScreenTestTags
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

    val accountIcon: KNode = child {

    }

    // AccountNameField
    // AccountColorMenu
    // AccountIconsList

}

internal fun BaseTestCase.onAccountInfoEditorScreen(function: AccountInfoEditPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)