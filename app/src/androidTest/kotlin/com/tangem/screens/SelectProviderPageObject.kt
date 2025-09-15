package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.SelectProviderBottomSheetTestTags
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class SelectProviderPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SelectProviderPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(R.string.onramp_choose_provider_title_hint))
        useUnmergedTree = true
    }

    val paymentMethodIcon: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.PAYMENT_METHOD_ICON)
        useUnmergedTree = true
    }

    val paymentMethodTitle: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.PAYMENT_METHOD_NAME)
        useUnmergedTree = true
    }

    val paymentMethodName: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.PAYMENT_METHOD_NAME)
        useUnmergedTree = true
    }

    val paymentMethodExpandButton: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.PAYMENT_METHOD_EXPAND_BUTTON)
        useUnmergedTree = true
    }

    val availableProviderItem: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.AVAILABLE_PROVIDER_ITEM)
        useUnmergedTree = true
    }

    val availableProviderName: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.AVAILABLE_PROVIDER_NAME)
        useUnmergedTree = true
    }

    val tokenAmount: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.TOKEN_AMOUNT)
        useUnmergedTree = true
    }

    val unavailableProviderItem: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.UNAVAILABLE_PROVIDER_ITEM)
        useUnmergedTree = true
    }

    val unavailableProviderName: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.UNAVAILABLE_PROVIDER_NAME)
        useUnmergedTree = true
    }

    val moreProvidersIcon: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.MORE_PROVIDERS_ICON)
        useUnmergedTree = true
    }

    val moreProvidersText: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.MORE_PROVIDERS_TEXT)
        useUnmergedTree = true
    }

    val bestRateLabel: KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.BEST_RATE_LABEL)
        useUnmergedTree = true
    }

    fun availableProviderWithName(name: String, tokenAmount: String, rate: String): KNode = child {
        hasTestTag(SelectProviderBottomSheetTestTags.AVAILABLE_PROVIDER_ITEM)
        hasAnyChild(withText(name))
        hasAnyChild(withText(tokenAmount))
        hasAnyChild(withText(rate))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSelectProviderBottomSheet(function: SelectProviderPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)