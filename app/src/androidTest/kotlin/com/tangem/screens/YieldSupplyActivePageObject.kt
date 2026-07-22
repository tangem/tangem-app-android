package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.NotificationTestTags
import com.tangem.core.ui.test.YieldSupplyTestTags
import com.tangem.core.res.R as CoreResR
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class YieldSupplyActivePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<YieldSupplyActivePageObject>(semanticsProvider = semanticsProvider) {

    val stopEarningButton: KNode = child {
        hasTestTag(YieldSupplyTestTags.STOP_EARNING_BUTTON)
        useUnmergedTree = true
    }

    val approveButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(CoreResR.string.yield_module_approve_needed_notification_cta))
        useUnmergedTree = true
    }

    fun notificationTitle(title: String): KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(title)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onYieldSupplyActiveScreen(function: YieldSupplyActivePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)