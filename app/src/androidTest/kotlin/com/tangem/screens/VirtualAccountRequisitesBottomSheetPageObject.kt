package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.VirtualAccountRequisitesBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class VirtualAccountRequisitesBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<VirtualAccountRequisitesBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val accountNumber: KNode = requisiteValue("Account number")

    val routingNumber: KNode = requisiteValue("Routing number")

    val bankName: KNode = requisiteValue("Bank name")

    // The row keys are the sheet's non-localized `titleForShare` values, not the visible labels.
    private fun requisiteValue(field: String): KNode = child {
        hasTestTag(VirtualAccountRequisitesBottomSheetTestTags.value(field))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onVirtualAccountRequisitesBottomSheet(
    function: VirtualAccountRequisitesBottomSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)