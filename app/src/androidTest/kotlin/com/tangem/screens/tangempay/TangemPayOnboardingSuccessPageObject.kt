package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.StatusScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayOnboardingSuccessPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayOnboardingSuccessPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(StatusScreenTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.tangempay_onboarding_success_title))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_close))
        hasClickAction()
    }
}

internal fun BaseTestCase.onTangemPayOnboardingSuccessScreen(
    function: TangemPayOnboardingSuccessPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)