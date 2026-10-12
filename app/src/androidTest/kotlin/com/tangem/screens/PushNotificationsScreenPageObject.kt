package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText
import com.tangem.core.res.R as CoreResR

class PushNotificationsScreenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<PushNotificationsScreenPageObject>(semanticsProvider = semanticsProvider) {

    val allowButton: KNode = child {
        hasClickAction()
        hasAnyDescendant(withText(getResourceString(CoreResR.string.common_allow)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onPushNotificationsScreen(function: PushNotificationsScreenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)