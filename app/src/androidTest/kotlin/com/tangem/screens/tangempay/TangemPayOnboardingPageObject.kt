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
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class TangemPayOnboardingPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayOnboardingPageObject>(semanticsProvider = semanticsProvider) {

    // TWI_1525_MONEY_ACCOUNTS_ENABLED swaps the screen for the unified layout with its own title and CTA.
    val title: KNode = child {
        addSemanticsMatcher(
            withText(getResourceString(CoreResR.string.tangempay_newonboard_title))
                .or(withText(getResourceString(CoreResR.string.tangempay_unified_onboarding_description))),
        )
        useUnmergedTree = true
    }

    val getCardButton: KNode = child {
        addSemanticsMatcher(
            withTestTag(BaseButtonTestTags.TEXT).and(
                withText(getResourceString(CoreResR.string.tangempay_onboarding_get_card_button_text))
                    .or(withText(getResourceString(CoreResR.string.common_get_started))),
            ),
        )
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