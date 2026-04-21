package com.tangem.screens.accounts

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.core.ui.test.accounts.AccountRowTestTags
import com.tangem.core.ui.test.accounts.ArchivedAccountsScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class ArchivedAccountsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ArchivedAccountsPageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(ArchivedAccountsScreenTestTags.ARCHIVED_ACCOUNTS_SCREEN_CONTAINER)
    }

    val topAppBarBackButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
    }

    val topAppBarTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.account_archived_accounts))
        useUnmergedTree = true
    }

    /**
     * Returns a composite handle for a single archived account row, scoped by account name.
     * All sub-elements (icon, title, tokens, networks, restore button) are children of this row.
     */
    fun findArchivedAccountItemByName(accountName: String): ArchivedAccountRow {
        val container: KNode = child {
            hasTestTag(ArchivedAccountsScreenTestTags.ARCHIVED_ACCOUNT_ITEM)
            hasAnyDescendant(withText(accountName))
            useUnmergedTree = true
        }
        return ArchivedAccountRow(container)
    }

    class ArchivedAccountRow(val container: KNode) {

        val icon: KNode = container.child {
            hasTestTag(AccountRowTestTags.ICON)
            useUnmergedTree = true
        }

        val title: KNode = container.child {
            hasTestTag(AccountRowTestTags.TITLE)
            useUnmergedTree = true
        }

        val subtitle: KNode = container.child {
            hasTestTag(AccountRowTestTags.SUBTITLE)
            useUnmergedTree = true
        }

        val restoreButton: KNode = container.child {
            hasTestTag(ArchivedAccountsScreenTestTags.RESTORE_BUTTON)
            useUnmergedTree = true
        }
    }
}

internal fun BaseTestCase.onArchivedAccountsScreen(function: ArchivedAccountsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)
