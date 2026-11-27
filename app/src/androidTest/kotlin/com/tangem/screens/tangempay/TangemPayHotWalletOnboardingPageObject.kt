package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class TangemPayHotWalletOnboardingPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayHotWalletOnboardingPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_unified_onboarding_description))
        useUnmergedTree = true
    }

    val getCardButton: KNode = child {
        addSemanticsMatcher(
            withTestTag(BaseButtonTestTags.TEXT).and(withText(getResourceString(CoreResR.string.common_get_started))),
        )
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayHotWalletOnboardingScreen(
    function: TangemPayHotWalletOnboardingPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)