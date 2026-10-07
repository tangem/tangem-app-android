package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.getQuantityString
import com.tangem.core.res.R as CoreResR
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayOrderCardSuccessPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayOrderCardSuccessPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_order_data_success_title))
        useUnmergedTree = true
    }

    val showCardButton: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_order_success_show_card))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_close))
        useUnmergedTree = true
    }

    fun deliveryEta(maxBusinessDays: Int): KNode = child {
        hasText(
            getQuantityString(CoreResR.plurals.tangempay_order_success_delivery_eta, maxBusinessDays, maxBusinessDays),
        )
        useUnmergedTree = true
    }

    fun emailNote(email: String): KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_order_success_email_note, email))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayOrderCardSuccessScreen(
    function: TangemPayOrderCardSuccessPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)