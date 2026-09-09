package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayHotWalletOnboardingPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayHotWalletOnboardingPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_onboarding_title))
        useUnmergedTree = true
    }

    // The Tangem Pay onboarding screen carries the same title and CTA — only this block is unique here.
    val setupWalletBlockTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_onboarding_setup_wallet_title))
        useUnmergedTree = true
    }

    val getCardButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(CoreResR.string.tangempay_onboarding_get_card_button_text))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayHotWalletOnboardingScreen(
    function: TangemPayHotWalletOnboardingPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)