package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.features.onboarding.v2.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class StoriesPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<StoriesPageObject>(semanticsProvider = semanticsProvider) {

    val getStartedButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_get_started))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onStoriesScreen(function: StoriesPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)