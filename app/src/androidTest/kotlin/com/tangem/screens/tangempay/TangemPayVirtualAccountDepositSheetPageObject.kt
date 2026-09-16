package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayVirtualAccountDepositSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayVirtualAccountDepositSheetPageObject>(semanticsProvider = semanticsProvider) {

    val showDetailsButton: KNode = child {
        hasTestTag(TangemPayTestTags.VA_DEPOSIT_SHOW_DETAILS_BUTTON)
        useUnmergedTree = true
    }

    val feeErrorBanner: KNode = child {
        hasTestTag(TangemPayTestTags.VA_DEPOSIT_FEE_ERROR_BANNER)
        useUnmergedTree = true
    }

    val feeErrorTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_bank_transfer_fee_error_title))
        useUnmergedTree = true
    }

    fun feeTitle(feeType: String): KNode = child {
        hasTestTag(TangemPayTestTags.vaDepositFeeTitle(feeType))
        useUnmergedTree = true
    }

    fun feeValue(feeType: String): KNode = child {
        hasTestTag(TangemPayTestTags.vaDepositFeeValue(feeType))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayVirtualAccountDepositSheet(
    function: TangemPayVirtualAccountDepositSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)