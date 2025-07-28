package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.StoriesScreenTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KButton

class StoriesPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<StoriesPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(StoriesScreenTestTags.SCREEN_CONTAINER) }
    ) {

    val scanButton: KNode = child {
        hasTestTag(StoriesScreenTestTags.SCAN_BUTTON)
    }

    val orderButton: KNode = child {
        hasTestTag(StoriesScreenTestTags.ORDER_BUTTON)
    }

    val enableNFCAlert: KView = KView {
        withId(R.id.alertTitle)
    }

    val cancelButton: KButton = KButton {
        withId(android.R.id.button2)
    }
}

internal fun BaseTestCase.onStoriesScreen(function: StoriesPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)