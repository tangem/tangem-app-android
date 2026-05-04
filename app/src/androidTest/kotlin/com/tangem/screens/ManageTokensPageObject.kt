package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.BaseSearchBarTestTags
import com.tangem.core.ui.test.ManageTokensScreenTestTags
import com.tangem.core.ui.test.SwitchTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText
import androidx.compose.ui.test.hasAnySibling as withAnySibling
import androidx.compose.ui.test.hasAnyDescendant as withAnyDescendant
import androidx.compose.ui.test.hasAnyAncestor as withAnyAncestor

class ManageTokensPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ManageTokensPageObject>(semanticsProvider = semanticsProvider) {

    val searchField: KNode = child {
        hasTestTag(BaseSearchBarTestTags.SEARCH_BAR)
    }

    fun tokenItem(tokenName: String): KNode = child {
        hasTestTag(ManageTokensScreenTestTags.TOKEN_ITEM)
        hasText(tokenName)
    }

    fun networkSwitch(networkName: String): KNode = child {
        useUnmergedTree = true
        addSemanticsMatcher(
            withTestTag(SwitchTestTags.SWITCH)
                .and(
                    withAnyAncestor(
                        withAnySibling(
                            withTestTag(ManageTokensScreenTestTags.NETWORK_NAME)
                                .and(withAnyDescendant(withText(networkName)))
                        )
                    )
                )
        )
    }

    val saveButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_save))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onManageTokensScreen(function: ManageTokensPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)