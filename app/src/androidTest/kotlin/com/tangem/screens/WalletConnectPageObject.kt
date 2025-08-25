package com.tangem.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.R
import com.tangem.core.ui.test.WalletConnectScreenTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class WalletConnectPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<WalletConnectPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(R.string.wallet_connect_title))
        useUnmergedTree = true
    }

    val emptyScreenImage: KNode = child {
        hasTestTag(WalletConnectScreenTestTags.EMPTY_SCREEN_IMAGE)
        useUnmergedTree = true
    }

    val emptyScreenText: KNode = child {
        hasTestTag(WalletConnectScreenTestTags.EMPTY_SCREEN_TEXT)
        useUnmergedTree = true
    }

    val addSessionButton: KNode = child {
        hasTestTag(WalletConnectScreenTestTags.ADD_SESSION_BUTTON)
        useUnmergedTree = true
    }

    private val sessionsLazyList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(WalletConnectScreenTestTags.SESSIONS_LAZY_LIST) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    @OptIn(ExperimentalTestApi::class)
    fun sessionTitle(sessionName: String): LazyListItemNode = sessionsLazyList.childWith<LazyListItemNode> {
        hasTestTag(WalletConnectScreenTestTags.SESSION_TITLE)
        hasText(sessionName)
        useUnmergedTree = true
    }

    @OptIn(ExperimentalTestApi::class)
    fun sessionCloseButton(sessionName: String): LazyListItemNode = sessionsLazyList.childWith<LazyListItemNode> {
        hasTestTag(WalletConnectScreenTestTags.CLOSE_SESSION_BUTTON)
        hasAnySibling(withText(sessionName))
    }
}

internal fun BaseTestCase.onWalletConnectScreen(function: WalletConnectPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)