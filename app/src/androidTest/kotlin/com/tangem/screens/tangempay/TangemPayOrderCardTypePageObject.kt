package com.tangem.screens.tangempay

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

/** 'Order new card' screen — the card-type step that precedes the issue bottom sheet. */
class TangemPayOrderCardTypePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayOrderCardTypePageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_order_type_title))
        useUnmergedTree = true
    }

    val virtualTab: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_order_type_segment_virtual))
        useUnmergedTree = true
    }

    val virtualTabItem: KNode = tabItem(CoreResR.string.tangempay_order_type_segment_virtual)

    val plasticTabItem: KNode = tabItem(CoreResR.string.tangempay_order_type_segment_plastic)

    val plusBackground: KNode = child {
        hasTestTag(TangemPayTestTags.ORDER_CARD_TYPE_BACKGROUND_PLUS)
        useUnmergedTree = true
    }

    val basicBackground: KNode = child {
        hasTestTag(TangemPayTestTags.ORDER_CARD_TYPE_BACKGROUND_BASIC)
        useUnmergedTree = true
    }

    val issueFeeTitle: KNode = text(getResourceString(CoreResR.string.tangempay_order_type_issue_fee))

    val deliveryTimeTitle: KNode = text(getResourceString(CoreResR.string.tangempay_order_type_delivery_time))

    val instantDeliveryTime: KNode =
        text(getResourceString(CoreResR.string.tangempay_order_type_delivery_time_instant))

    val deliverToTitle: KNode = text(getResourceString(CoreResR.string.tangempay_order_type_delivery_to))

    val countryOfResidenceHint: KNode =
        text(getResourceString(CoreResR.string.tangempay_order_type_country_of_residence))

    val deliveryFeeTitle: KNode = text(getResourceString(CoreResR.string.tangempay_order_type_delivery_fee))

    val firstDeliveryFreeBadge: KNode =
        text(getResourceString(CoreResR.string.tangempay_order_type_first_delivery_free))

    val notEnoughMoneyBadge: KNode = text(getResourceString(CoreResR.string.tangempay_order_type_not_enough_money))

    val unavailableBadge: KNode = text(getResourceString(CoreResR.string.tangempay_order_type_unavailable))

    val selectButton: KNode = child {
        hasTestTag(TangemPayTestTags.ORDER_CARD_TYPE_SELECT_BUTTON)
        useUnmergedTree = true
    }

    fun text(text: String): KNode = child {
        hasText(text)
        useUnmergedTree = true
    }

    fun deliveryEtaRange(minDays: Int, maxDays: Int): KNode =
        text(getResourceString(CoreResR.string.tangempay_order_type_delivery_eta_range, minDays, maxDays))

    fun noDataValueAt(index: Int): KNode = child {
        hasText(DASH_SIGN)
        hasPosition(index)
        useUnmergedTree = true
    }

    private fun tabItem(labelRes: Int): KNode = child {
        hasText(getResourceString(labelRes))
        addSemanticsMatcher(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
    }
}

internal fun BaseTestCase.onTangemPayOrderCardTypeScreen(function: TangemPayOrderCardTypePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)