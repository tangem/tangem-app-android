package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.OnrampOffersBlockTestTags
import com.tangem.core.ui.test.SelectPaymentMethodBottomSheetTestTags
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class SelectPaymentMethodPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SelectPaymentMethodPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.TITLE)
        hasText(getResourceString(R.string.onramp_all_offers_button_title))
    }

    val subtitle: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.SUBTITLE)
        hasText(getResourceString(R.string.onramp_payment_method_subtitle))
    }

    val closeButton: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.CLOSE_BUTTON)
    }

    fun paymentMethodWithName(name: String): KNode = child {
        hasTestTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD)
        hasAnyDescendant(withText(name))
        hasAnyDescendant(withTestTag(SelectPaymentMethodBottomSheetTestTags.PAYMENT_METHOD_ICON))
        hasAnyDescendant(withTestTag(SelectPaymentMethodBottomSheetTestTags.TIMING_ICON))
        hasAnyDescendant(withTestTag(OnrampOffersBlockTestTags.TIMING_TEXT))
        hasAnyDescendant(withTestTag(SelectPaymentMethodBottomSheetTestTags.TOKEN_AMOUNT))
        hasAnyDescendant(withTestTag(SelectPaymentMethodBottomSheetTestTags.PROVIDER_COUNT_ICON))
        hasAnyDescendant(withTestTag(SelectPaymentMethodBottomSheetTestTags.PROVIDER_COUNT_TEXT))
        hasAnyDescendant(withTestTag(SelectPaymentMethodBottomSheetTestTags.UP_TO_TEXT))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSelectPaymentMethodBottomSheet(function: SelectPaymentMethodPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)