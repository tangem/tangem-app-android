package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayOnboardingPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayOnboardingPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_onboarding_title))
        useUnmergedTree = true
    }

    val getCardButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(CoreResR.string.tangempay_onboarding_get_card_button_text))
        useUnmergedTree = true
    }

    val unavailableRegionIcon: KNode = child {
        hasTestTag(TangemPayTestTags.ONBOARDING_UNAVAILABLE_ICON)
        useUnmergedTree = true
    }

    val unavailableRegionTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangem_pay_unavailable_region_title))
    }

    val unavailableRegionDescription: KNode = child {
        hasText(getResourceString(CoreResR.string.tangem_pay_unavailable_region_description))
    }

    val gotItButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_got_it))
    }
}

internal fun BaseTestCase.onTangemPayOnboardingScreen(function: TangemPayOnboardingPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)