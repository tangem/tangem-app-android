package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.OnrampOffersBlockTestTags
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.core.ui.R as CoreUiR

class SelectProviderPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SelectProviderPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.TITLE)
        hasText(getResourceString(R.string.onramp_all_offers_button_title))
    }

    val subtitle: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.SUBTITLE)
        hasText(getResourceString(R.string.express_provider))
        useUnmergedTree = true
    }

    val bestRateIcon: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.BEST_RATE_ICON)
        useUnmergedTree = true
    }

    val bestRateTitle: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.BEST_RATE_TITLE)
        useUnmergedTree = true
    }

    val offerTokenAmount: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.OFFER_TOKEN_AMOUNT)
        useUnmergedTree = true
    }

    val timingIcon: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.TIMING_ICON)
        useUnmergedTree = true
    }

    val payWith: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.PAY_WITH)
        useUnmergedTree = true
    }

    val youGetText: KNode = child {
        hasText(getResourceString(R.string.onramp_title_you_get))
    }

    val paymentMethodIcon: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.PAYMENT_METHOD_ICON)
        useUnmergedTree = true
    }

    val providerName: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.PROVIDER_NAME)
        useUnmergedTree = true
    }


    val instantProcessingSpeedText: KNode = child {
        hasText(getResourceString(com.tangem.core.ui.R.string.onramp_instant_status))
        useUnmergedTree = true
    }

    val buyButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(CoreUiR.string.common_buy))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSelectProviderBottomSheet(function: SelectProviderPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)