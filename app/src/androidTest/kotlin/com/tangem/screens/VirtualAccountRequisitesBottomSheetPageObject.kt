package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.VirtualAccountRequisitesBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class VirtualAccountRequisitesBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<VirtualAccountRequisitesBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val accountNumber: KNode = requisiteValue(ACCOUNT_NUMBER)

    val routingNumber: KNode = requisiteValue(ROUTING_NUMBER)

    val bankName: KNode = requisiteValue(BANK_NAME)

    val shareButton: KNode = child {
        hasTestTag(VirtualAccountRequisitesBottomSheetTestTags.SHARE_BUTTON)
        useUnmergedTree = true
    }

    val depositLimitTitle: KNode = child {
        hasTestTag(VirtualAccountRequisitesBottomSheetTestTags.DEPOSIT_LIMIT_TITLE)
        useUnmergedTree = true
    }

    val depositLimitSubtitle: KNode = child {
        hasTestTag(VirtualAccountRequisitesBottomSheetTestTags.DEPOSIT_LIMIT_SUBTITLE)
        useUnmergedTree = true
    }

    // The row keys are the sheet's non-localized `titleForShare` values, not the visible labels.
    fun requisiteValue(field: String): KNode = child {
        hasTestTag(VirtualAccountRequisitesBottomSheetTestTags.value(field))
        useUnmergedTree = true
    }

    fun copyButton(field: String): KNode = child {
        hasTestTag(VirtualAccountRequisitesBottomSheetTestTags.copyButton(field))
        useUnmergedTree = true
    }

    companion object {
        const val BENEFICIARY_NAME = "Beneficiary name"
        const val BENEFICIARY_ADDRESS = "Beneficiary address"
        const val BANK_NAME = "Bank name"
        const val BANK_ADDRESS = "Bank address"
        const val ACCOUNT_NUMBER = "Account number"
        const val ROUTING_NUMBER = "Routing number"
    }
}

internal fun BaseTestCase.onVirtualAccountRequisitesBottomSheet(
    function: VirtualAccountRequisitesBottomSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)