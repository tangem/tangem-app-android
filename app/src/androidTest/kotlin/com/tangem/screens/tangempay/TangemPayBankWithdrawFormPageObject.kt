package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasSetTextAction as withSetTextAction
import androidx.compose.ui.test.hasTestTag as withTestTag

enum class TangemPayBankWithdrawField {
    AccountNumber,
    RoutingNumber,
    FirstName,
    LastName,
    BusinessName,
    Country,
    Region,
    City,
    AddressLine1,
    PostalCode,
    BankName,
    BankRegion,
    BankCity,
    BankAddressLine1,
    BankPostalCode,
}

class TangemPayBankWithdrawFormPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayBankWithdrawFormPageObject>(semanticsProvider = semanticsProvider) {

    val continueButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_continue))
        hasClickAction()
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_FORM_SCREEN))
    }

    fun field(field: TangemPayBankWithdrawField): KNode = child {
        hasTestTag(TangemPayTestTags.bankWithdrawFormField(field.name))
        useUnmergedTree = true
    }

    fun input(field: TangemPayBankWithdrawField): KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.bankWithdrawFormField(field.name)))
        addSemanticsMatcher(withSetTextAction())
        useUnmergedTree = true
    }

    fun valueOf(field: TangemPayBankWithdrawField, value: String): KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.bankWithdrawFormField(field.name)))
        hasText(value)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayBankWithdrawFormScreen(
    function: TangemPayBankWithdrawFormPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)