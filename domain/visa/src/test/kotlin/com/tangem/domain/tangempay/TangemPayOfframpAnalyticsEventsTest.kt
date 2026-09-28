package com.tangem.domain.tangempay

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayOfframpAnalyticsEventsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN offramp event WHEN created THEN id and params are correct`(model: EventModel) {
        // Act
        val event = model.event

        // Assert
        assertThat(event.id).isEqualTo(model.expectedId)
        assertThat(event.params).isEqualTo(model.expectedParams)
    }

    @Test
    fun `GIVEN offramp events WHEN created THEN category is Visa VA Offramp`() {
        // Act
        val categories = provideTestModels().map { it.event.category }.distinct()

        // Assert
        assertThat(categories).containsExactly(CATEGORY)
    }

    internal data class EventModel(
        val event: TangemPayAnalyticsEvents,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    )

    private fun provideTestModels() = listOf(
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.BankTransferSheetOpened(),
            expectedId = "[$CATEGORY] Bank Transfer Sheet Opened",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.ToPersonalAccountClicked(),
            expectedId = "[$CATEGORY] To Personal Account Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.ToBusinessAccountClicked(),
            expectedId = "[$CATEGORY] To Business Account Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.SavedContactClicked(),
            expectedId = "[$CATEGORY] Saved Contact Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.SavedContactsSeeAllClicked(),
            expectedId = "[$CATEGORY] Saved Contacts See All Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.SavedContactsScreenOpened(),
            expectedId = "[$CATEGORY] Saved Contacts Screen Opened",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.SearchBarClicked(),
            expectedId = "[$CATEGORY] Search Bar Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.SavedContactOnContactsScreenClicked(),
            expectedId = "[$CATEGORY] Saved Contact On Contacts Screen Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.AmountEntered(),
            expectedId = "[$CATEGORY] Amount Entered",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.CoinSelectorClicked(),
            expectedId = "[$CATEGORY] Coin Selector Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.CoinSelectionSheetOpened(),
            expectedId = "[$CATEGORY] Coin Selection Sheet Opened",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.CoinInSelectionSheetClicked(),
            expectedId = "[$CATEGORY] Coin In Selection Sheet Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.AmountContinueClicked(),
            expectedId = "[$CATEGORY] Amount Continue Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.PersonalAccountTypeScreenOpened(),
            expectedId = "[$CATEGORY] Personal Account Type Screen Opened",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.PersonalAccountTypeContinueClicked(isThirdParty = true),
            expectedId = "[$CATEGORY] Personal Account Type Continue Clicked",
            expectedParams = mapOf(AnalyticsParam.Key.TYPE to "3rd party"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.PersonalAccountTypeContinueClicked(isThirdParty = false),
            expectedId = "[$CATEGORY] Personal Account Type Continue Clicked",
            expectedParams = mapOf(AnalyticsParam.Key.TYPE to "1st party"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RecipientDetailsScreenOpened(),
            expectedId = "[$CATEGORY] Recipient Details Screen Opened",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RecipientDetailsFieldFilled(field = "Full Name"),
            expectedId = "[$CATEGORY] Recipient Details Field Filled",
            expectedParams = mapOf(AnalyticsParam.Key.FIELD to "Full Name"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RecipientDetailsContinueClicked(),
            expectedId = "[$CATEGORY] Recipient Details Continue Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RecipientBankDetailsScreenOpened(),
            expectedId = "[$CATEGORY] Recipient Bank Details Screen Opened",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RecipientBankDetailsFieldFilled(field = "Account Number"),
            expectedId = "[$CATEGORY] Recipient Bank Details Field Filled",
            expectedParams = mapOf(AnalyticsParam.Key.FIELD to "Account Number"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RecipientBankDetailsContinueClicked(),
            expectedId = "[$CATEGORY] Recipient Bank Details Continue Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.ConfirmationScreenOpened(),
            expectedId = "[$CATEGORY] Confirmation Screen Opened",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RailSelectorClicked(),
            expectedId = "[$CATEGORY] Rail Selector Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RecipientDetailsOnConfirmationClicked(),
            expectedId = "[$CATEGORY] Recipient Details On Confirmation Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RecipientBankDetailsOnConfirmationClicked(),
            expectedId = "[$CATEGORY] Recipient Bank Details On Confirmation Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RailSelectionSheetOpened(),
            expectedId = "[$CATEGORY] Rail Selection Sheet Opened",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.RailInSelectionSheetClicked(),
            expectedId = "[$CATEGORY] Rail In Selection Sheet Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.InsufficientFundsStateShowed(),
            expectedId = "[$CATEGORY] Insufficient Funds State Showed",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked(),
            expectedId = "[$CATEGORY] Send On Confirmation Clicked",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened(),
            expectedId = "[$CATEGORY] Send Error Screen Opened",
            expectedParams = emptyMap(),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.SendSuccessScreenShowed(rail = BankRail.ACH),
            expectedId = "[$CATEGORY] Send Success Screen Showed",
            expectedParams = mapOf(AnalyticsParam.Key.TYPE to "ACH"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.SendSuccessScreenShowed(rail = BankRail.WIRE),
            expectedId = "[$CATEGORY] Send Success Screen Showed",
            expectedParams = mapOf(AnalyticsParam.Key.TYPE to "Wire"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Offramp.SendSuccessScreenShowed(rail = null),
            expectedId = "[$CATEGORY] Send Success Screen Showed",
            expectedParams = emptyMap(),
        ),
    )

    private companion object {
        const val CATEGORY = "Visa VA Offramp"
    }
}