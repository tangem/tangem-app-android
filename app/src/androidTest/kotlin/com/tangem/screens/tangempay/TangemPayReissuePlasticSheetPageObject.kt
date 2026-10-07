package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.getQuantityString
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayReissuePlasticSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayReissuePlasticSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = text(getResourceString(CoreResR.string.tangempay_reissue_plastic_title))

    val description: KNode = text(getResourceString(CoreResR.string.tangempay_reissue_plastic_description))

    val deliverToTitle: KNode = text(getResourceString(CoreResR.string.tangempay_order_type_delivery_to))

    val deliveryFeeTitle: KNode = text(getResourceString(CoreResR.string.tangempay_order_type_delivery_fee))

    val deliveryTimeTitle: KNode = text(getResourceString(CoreResR.string.tangempay_order_type_delivery_time))

    val notEnoughMoneyBadge: KNode = text(getResourceString(CoreResR.string.tangempay_order_type_not_enough_money))

    val cancelButton: KNode = child {
        hasTestTag(TangemPayTestTags.REISSUE_PLASTIC_SHEET_CANCEL_BUTTON)
        useUnmergedTree = true
    }

    val replaceButton: KNode = child {
        hasTestTag(TangemPayTestTags.REISSUE_PLASTIC_SHEET_REPLACE_BUTTON)
        useUnmergedTree = true
    }

    fun text(text: String): KNode = child {
        hasText(text)
        useUnmergedTree = true
    }

    fun deliveryTime(maxBusinessDays: Int): KNode =
        text(getQuantityString(CoreResR.plurals.tangempay_order_type_delivery_eta, maxBusinessDays, maxBusinessDays))
}

internal fun BaseTestCase.onTangemPayReissuePlasticSheet(
    function: TangemPayReissuePlasticSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)