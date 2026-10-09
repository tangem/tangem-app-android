package com.tangem.screens.tangempay

import androidx.compose.ui.semantics.SemanticsActions
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

enum class TangemPayOrderDataField(val testTag: String) {
    NameOnCard(TangemPayTestTags.ORDER_DATA_NAME_ON_CARD_FIELD),
    Country(TangemPayTestTags.ORDER_DATA_COUNTRY_FIELD),
    Email(TangemPayTestTags.ORDER_DATA_EMAIL_FIELD),
    FirstName(TangemPayTestTags.ORDER_DATA_FIRST_NAME_FIELD),
    LastName(TangemPayTestTags.ORDER_DATA_LAST_NAME_FIELD),
    Region(TangemPayTestTags.ORDER_DATA_REGION_FIELD),
    City(TangemPayTestTags.ORDER_DATA_CITY_FIELD),
    AddressLine1(TangemPayTestTags.ORDER_DATA_ADDRESS_LINE1_FIELD),
    AddressLine2(TangemPayTestTags.ORDER_DATA_ADDRESS_LINE2_FIELD),
    PostalCode(TangemPayTestTags.ORDER_DATA_POSTAL_CODE_FIELD),
    Phone(TangemPayTestTags.ORDER_DATA_PHONE_FIELD),
}

class TangemPayOrderCardDataPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayOrderCardDataPageObject>(semanticsProvider = semanticsProvider) {

    val subtitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_order_data_subtitle))
        useUnmergedTree = true
    }

    val orderButton: KNode = child {
        hasTestTag(TangemPayTestTags.ORDER_DATA_ORDER_BUTTON)
        useUnmergedTree = true
    }

    fun input(field: TangemPayOrderDataField): KNode = child {
        hasAnyAncestor(withTestTag(field.testTag))
        addSemanticsMatcher(withSetTextAction())
        useUnmergedTree = true
    }

    fun focus(field: TangemPayOrderDataField) {
        input(field).performScrollTo()
        input(field).performSemanticsAction(SemanticsActions.RequestFocus)
    }

    fun valueOf(field: TangemPayOrderDataField, value: String): KNode = child {
        hasAnyAncestor(withTestTag(field.testTag))
        hasText(value)
        useUnmergedTree = true
    }

    fun requiredError(field: TangemPayOrderDataField): KNode =
        error(field, getResourceString(CoreResR.string.tangempay_order_data_field_required))

    fun latinOnlyError(field: TangemPayOrderDataField): KNode =
        error(field, getResourceString(CoreResR.string.tangempay_order_data_field_latin_only))

    private fun error(field: TangemPayOrderDataField, text: String): KNode = child {
        hasAnyAncestor(withTestTag(field.testTag))
        hasText(text)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayOrderCardDataScreen(function: TangemPayOrderCardDataPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)