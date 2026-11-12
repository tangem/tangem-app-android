package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.YieldSupplyTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class YieldSupplyPromoPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<YieldSupplyPromoPageObject>(semanticsProvider = semanticsProvider) {

    val continueButton: KNode = child {
        hasTestTag(YieldSupplyTestTags.PROMO_CONTINUE_BUTTON)
        useUnmergedTree = true
    }

    fun incentiveChip(rewardsApy: String): KNode = child {
        hasText(getResourceString(CoreResR.string.yield_module_promo_screen_incentivised_chip, rewardsApy))
        useUnmergedTree = true
    }

    fun title(apy: String): KNode = child {
        hasText(getResourceString(CoreResR.string.yield_module_promo_screen_title_v2, apy))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onYieldSupplyPromoScreen(function: YieldSupplyPromoPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)