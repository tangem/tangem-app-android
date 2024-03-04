package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.tap.common.compose.resources.C
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KButton

class StoriesScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<StoriesScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(C.Tag.STORIES_SCREEN) }
    ) {

    val scanButton: KNode = child {
        hasTestTag(C.Tag.STORIES_SCREEN_SCAN_BUTTON)
    }

    val orderButton: KNode = child {
        hasTestTag(C.Tag.STORIES_SCREEN_ORDER_BUTTON)
    }

    val enableNFCAlert: KView = KView {
        withId(R.id.alertTitle)
    }

    val cancelButton: KButton = KButton {
        withId(android.R.id.button2)
    }
}