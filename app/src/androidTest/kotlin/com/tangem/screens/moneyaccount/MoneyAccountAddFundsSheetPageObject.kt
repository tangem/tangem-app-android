package com.tangem.screens.moneyaccount

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.MoneyAccountTestTags
import io.github.kakaocup.compose.intercept.operation.ComposeOperationType
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag

class MoneyAccountAddFundsSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MoneyAccountAddFundsSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = sheetText(CoreResR.string.common_add_funds)

    val swapOption: KNode = sheetText(CoreResR.string.tangempay_topup_swap_title)

    val swapOptionDescription: KNode = sheetText(CoreResR.string.money_account_add_funds_swap_description)

    val receiveOption: KNode = sheetText(CoreResR.string.tangempay_topup_receive_title)

    val receiveOptionDescription: KNode = sheetText(CoreResR.string.money_account_add_funds_receive_description)

    val bankTransferOption: KNode = sheetText(CoreResR.string.tangempay_topup_bank_transfer_title)

    val bankTransferOptionDescription: KNode = sheetText(CoreResR.string.tangempay_topup_bank_transfer_body)

    val bankTransferLoader: KNode = child {
        hasTestTag(MoneyAccountTestTags.ADD_FUNDS_OPTION_LOADER)
        useUnmergedTree = true
    }

    private val container: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.CONTAINER)
        useUnmergedTree = true
    }

    fun topOf(node: KNode): Float {
        var top = 0f
        node.delegate.perform(SheetAction.READ_BOUNDS) { top = fetchSemanticsNode().boundsInRoot.top }
        return top
    }

    fun dismissBySwipeDown() {
        container.delegate.perform(SheetAction.SWIPE_DOWN) { performTouchInput { swipeDown() } }
    }

    private fun sheetText(@StringRes textRes: Int): KNode = child {
        hasText(getResourceString(textRes))
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
        useUnmergedTree = true
    }

    private enum class SheetAction : ComposeOperationType { READ_BOUNDS, SWIPE_DOWN }
}

internal fun BaseTestCase.onMoneyAccountAddFundsSheet(function: MoneyAccountAddFundsSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)