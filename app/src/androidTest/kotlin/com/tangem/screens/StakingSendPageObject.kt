package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.StakingSendScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.features.send.v2.impl.R as SendR
import androidx.compose.ui.test.hasTestTag as withTestTag

class StakingSendPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<StakingSendPageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(StakingSendScreenTestTags.SCREEN_CONTAINER)
    }

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        useUnmergedTree = true
    }

    val amountContainerTitle: KNode = child {
        hasTestTag(StakingSendScreenTestTags.AMOUNT_CONTAINER_TITLE)
        useUnmergedTree = true
    }

    val amountContainerText: KNode = child {
        hasTestTag(StakingSendScreenTestTags.AMOUNT_CONTAINER_TEXT)
        useUnmergedTree = true
    }

    val amountInputTextField: KNode = child {
        hasTestTag(StakingSendScreenTestTags.INPUT_TEXT_FIELD)
        useUnmergedTree = true
    }

    val secondaryAmount: KNode = child {
        hasTestTag(StakingSendScreenTestTags.SECONDARY_AMOUNT)
        useUnmergedTree = true
    }

    val currencyButton: KNode = child {
        hasTestTag(StakingSendScreenTestTags.CURRENCY_BUTTON)
        hasAnyChild(withTestTag(StakingSendScreenTestTags.CURRENCY_ICON))
        useUnmergedTree = true
    }

    val fiatButton: KNode = child {
        hasTestTag(StakingSendScreenTestTags.CURRENCY_BUTTON)
        hasAnyChild(withTestTag(StakingSendScreenTestTags.FIAT_ICON))
        useUnmergedTree = true
    }

    val maxButton: KNode = child {
        hasTestTag(StakingSendScreenTestTags.MAX_BUTTON)
        useUnmergedTree = true
    }

    val previousButton: KNode = child {
        hasTestTag(StakingSendScreenTestTags.PREVIOUS_BUTTON)
        useUnmergedTree = true
    }

    val nextButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(SendR.string.common_next))
        useUnmergedTree = true
    }

}

internal fun BaseTestCase.onStakingSendScreen(function: StakingSendPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)