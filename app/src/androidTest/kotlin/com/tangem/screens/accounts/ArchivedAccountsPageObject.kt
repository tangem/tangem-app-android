// screens/accounts/ArchivedAccountsPageObject.kt — new file
package com.tangem.screens.accounts

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.accounts.ArchivedAccountsScreenTestTags // TODO: define tags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasText as withText
import androidx.compose.ui.test.hasTestTag as withTestTag

class ArchivedAccountsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ArchivedAccountsPageObject>(semanticsProvider = semanticsProvider) {

    fun findArchivedAccountItemByName(accountName: String): KNode = child {
        hasTestTag(ArchivedAccountsScreenTestTags.ARCHIVED_ACCOUNT_ITEM) // TODO: confirm tag
        hasAnyDescendant(withText(accountName))
        useUnmergedTree = true
    }

    fun restoreAccountByName(accountName: String): KNode = child {
        hasTestTag(ArchivedAccountsScreenTestTags.ARCHIVED_ACCOUNT_RESTORE_BUTTON) // TODO: confirm tag
        // If there's one restore button per account row, tie it to the account:
        // hasAnyAncestor(
        //     withTestTag(ArchivedAccountsScreenTestTags.ARCHIVED_ACCOUNT_ITEM)
        //         .and(withAnyDescendant(withText(accountName)))
        // )
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onArchivedAccountsScreen(function: ArchivedAccountsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)