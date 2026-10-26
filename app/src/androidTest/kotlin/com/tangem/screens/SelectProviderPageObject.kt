package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyDescendant as withAnyDescendant
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction as withClickAction
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.isEnabled as withEnabledState
import androidx.compose.ui.test.isNotEnabled as withDisabledState
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

private val providerSheetMatcher: SemanticsMatcher =
    withTestTag(BaseBottomSheetTestTags.CONTAINER) and withAnyDescendant(withTestTag(BaseBottomSheetTestTags.SUBTITLE))

class SelectProviderPageObject(private val provider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SelectProviderPageObject>(semanticsProvider = provider) {

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


    fun providerNameContaining(name: String): KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.PROVIDER_NAME)
        hasText(name, substring = true)
        hasAnyAncestor(providerSheetMatcher)
        useUnmergedTree = true
    }

    // Raw Compose on purpose: the sheet and the screen behind it share tags, so only bounds prove the order.
    fun assertOfferAmountsOrdered(higherAmount: String, lowerAmount: String) {
        val amountNodes = provider
            .onAllNodes(
                matcher = withTestTag(OnrampOffersBlockTestTags.OFFER_TOKEN_AMOUNT) and
                    hasAnyAncestor(providerSheetMatcher),
                useUnmergedTree = true,
            )
            .fetchSemanticsNodes()

        fun topsOf(amount: String): List<Float> = amountNodes
            .filter { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }
            .filter { it.config[SemanticsProperties.Text].joinToString().contains(amount) }
            .map { it.boundsInRoot.top }

        val higherTops = topsOf(higherAmount)
        val lowerTops = topsOf(lowerAmount)
        if (higherTops.isEmpty() || lowerTops.isEmpty() || higherTops.max() >= lowerTops.min()) {
            throw AssertionError(
                "Offers are not sorted: '$higherAmount' tops=$higherTops, '$lowerAmount' tops=$lowerTops",
            )
        }
    }

    val restrictedOfferBadge: KNode = child {
        hasText(getResourceString(CoreUiR.string.common_not_available))
        hasAnyAncestor(providerSheetMatcher)
        useUnmergedTree = true
    }

    val enabledOfferBuyButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(CoreUiR.string.common_buy))
        hasAnyAncestor(providerSheetMatcher)
        hasAnyAncestor(withEnabledState() and withClickAction())
        useUnmergedTree = true
    }

    val disabledOfferBuyButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(CoreUiR.string.common_buy))
        hasAnyAncestor(providerSheetMatcher)
        hasAnyAncestor(withDisabledState())
        useUnmergedTree = true
    }

    val instantProcessingSpeedText: KNode = child {
        hasText(getResourceString(R.string.onramp_instant_status))
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