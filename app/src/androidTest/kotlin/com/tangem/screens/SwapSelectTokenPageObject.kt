package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.*
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class SwapSelectTokenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapSelectTokenPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.common_swap))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
    }

    val youSwapTitle: KNode = child {
        hasText(getResourceString(R.string.swapping_from_title))
        useUnmergedTree = true
    }

    val youSwapBlock: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.SWAP_CARD)
        hasAnyDescendant(withText(getResourceString(R.string.swapping_from_title)))
        useUnmergedTree = true
    }

    val youReceiveTitle: KNode = child {
        hasText(getResourceString(R.string.swapping_to_title))
        useUnmergedTree = true
    }

    val youReceiveBlock: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.RECEIVE_CARD)
        hasAnyDescendant(withText(getResourceString(R.string.swapping_to_title)))
        useUnmergedTree = true
    }

    val searchBarBlock: KNode = child {
        hasTestTag(BaseSearchBarTestTags.SEARCH_BAR)
        useUnmergedTree = true
    }

    val searchBarIcon: KNode = child {
        hasTestTag(SearchBarTestTags.ICON)
        useUnmergedTree = true
    }

    val searchBarPlaceholderText: KNode = child {
        hasTestTag(SearchBarTestTags.PLACEHOLDER_TEXT)
        useUnmergedTree = true
    }

    val searchClearButton: KNode = child {
        hasTestTag(SearchBarTestTags.CLEAR_BUTTON)
        useUnmergedTree = true
    }

    val tryAgainButton: KNode = child {
        hasText(getResourceString(R.string.alert_button_try_again))
        useUnmergedTree = true
    }

    val unableToLoadData: KNode = child {
        hasText(getResourceString(R.string.markets_loading_error_title))
        useUnmergedTree = true
    }

    // Merged tree: the label is a grandchild Text node, only reachable via the chip's own merged semantics.
    val balanceFilterChip: KNode = child {
        hasTestTag(SwapSelectTokenScreenTestTags.BALANCE_FILTER_CHIP)
    }

    /** Dropdown row for a [com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilter] option. */
    fun balanceFilterOption(title: String): KNode = child {
        hasTestTag(SwapSelectTokenScreenTestTags.BALANCE_FILTER_OPTION)
        hasAnyDescendant(withText(title))
        useUnmergedTree = true
    }

    val filteredOutEmptyMessage: KNode = child {
        hasTestTag(SwapSelectTokenScreenTestTags.FILTERED_OUT_EMPTY_MESSAGE)
        useUnmergedTree = true
    }

    val seeAllButton: KNode = child {
        hasTestTag(SwapSelectTokenScreenTestTags.FILTERED_OUT_SEE_ALL_BUTTON)
        useUnmergedTree = true
    }

    fun tokenWithName(tokenName: String): KNode = child {
        hasTestTag(TokenElementsTestTags.TOKEN_TITLE)
        hasAnyChild(withText(tokenName))
        useUnmergedTree = true
    }

    /**
     * Picks a token row by name **and** network.
     *
     * A wallet can hold the same token on several networks — POL (ex-MATIC) exists both as an ERC-20 on
     * Ethereum and natively on Polygon — and [tokenWithName] matches whichever row comes first, so which
     * one gets tapped depends on list order. The merged row carries the network ("Ethereum network"), so
     * naming it makes the choice deterministic, and with it the pair, its providers and their quotes.
     */
    fun tokenWithNameAndNetwork(tokenName: String, networkName: String): KNode = child {
        hasText(tokenName, substring = true)
        hasText(networkName, substring = true)
        hasClickAction()
    }

    fun marketsTokenWithName(title: String): KNode {
        return child {
            hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM)
            hasText(title)
        }
    }
}

internal fun BaseTestCase.onSwapSelectTokenScreen(function: SwapSelectTokenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)