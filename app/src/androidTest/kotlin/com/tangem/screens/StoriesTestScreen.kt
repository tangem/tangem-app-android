package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.core.ui.test.TestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KButton

class StoriesTestScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<StoriesTestScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTags.STORIES_SCREEN) }
    ) {

    val scanButton: KNode = child {
        hasTestTag(TestTags.STORIES_SCREEN_SCAN_BUTTON)
    }

    val orderButton: KNode = child {
        hasTestTag(TestTags.STORIES_SCREEN_ORDER_BUTTON)
    }

    val enableNFCAlert: KView = KView {
        withId(R.id.alertTitle)
    }

    val cancelButton: KButton = KButton {
        withId(android.R.id.button2)
    }
}