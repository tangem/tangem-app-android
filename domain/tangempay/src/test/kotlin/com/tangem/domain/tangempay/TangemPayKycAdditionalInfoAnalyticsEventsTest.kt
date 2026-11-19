package com.tangem.domain.tangempay

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.AccountType
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.CloseMethod
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.ContactSupportClicked
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.EntryPoint
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.NoticeShown
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.NoticeState
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.Placement
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.PopupClosed
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.PopupShown
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.ProvideInformationClicked
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.Source
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class TangemPayKycAdditionalInfoAnalyticsEventsTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Contract {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN cdd event WHEN created THEN id and params match the spec`(model: EventModel) {
            // Act
            val event = model.event

            // Assert
            assertThat(event.id).isEqualTo(model.expectedId)
            assertThat(event.params).isEqualTo(model.expectedParams)
        }

        private fun provideTestModels() = listOf(
            EventModel(
                event = NoticeShown(accountType = AccountType.PAYMENT, placement = Placement.HOME_ROW),
                expectedId = "[Visa CDD/EDD] Notice Shown",
                expectedParams = mapOf(
                    "account_type" to "payment",
                    "kyc_status" to "in_progress",
                    "placement" to "home_row",
                ),
            ),
            EventModel(
                event = NoticeShown(accountType = AccountType.MONEY, placement = Placement.ACCOUNT_BLOCK),
                expectedId = "[Visa CDD/EDD] Notice Shown",
                expectedParams = mapOf(
                    "account_type" to "money",
                    "kyc_status" to "in_progress",
                    "placement" to "account_block",
                ),
            ),
            EventModel(
                event = PopupShown(accountType = AccountType.PAYMENT, entryPoint = EntryPoint.ACCOUNT_ENTRY),
                expectedId = "[Visa CDD/EDD] Popup Shown",
                expectedParams = mapOf(
                    "account_type" to "payment",
                    "kyc_status" to "in_progress",
                    "entry_point" to "account_entry",
                ),
            ),
            EventModel(
                event = PopupShown(accountType = AccountType.MONEY, entryPoint = EntryPoint.PUSH),
                expectedId = "[Visa CDD/EDD] Popup Shown",
                expectedParams = mapOf(
                    "account_type" to "money",
                    "kyc_status" to "in_progress",
                    "entry_point" to "push",
                ),
            ),
            EventModel(
                event = PopupClosed(
                    accountType = AccountType.MONEY,
                    entryPoint = EntryPoint.ACCOUNT_ENTRY,
                    closeMethod = CloseMethod.UNKNOWN,
                ),
                expectedId = "[Visa CDD/EDD] Popup Closed",
                expectedParams = mapOf(
                    "account_type" to "money",
                    "kyc_status" to "in_progress",
                    "entry_point" to "account_entry",
                    "close_method" to "unknown",
                ),
            ),
            EventModel(
                event = PopupClosed(
                    accountType = AccountType.PAYMENT,
                    entryPoint = EntryPoint.ACCOUNT_ENTRY,
                    closeMethod = CloseMethod.CLOSE_BUTTON,
                ),
                expectedId = "[Visa CDD/EDD] Popup Closed",
                expectedParams = mapOf(
                    "account_type" to "payment",
                    "kyc_status" to "in_progress",
                    "entry_point" to "account_entry",
                    "close_method" to "close_button",
                ),
            ),
            EventModel(
                event = ProvideInformationClicked(
                    accountType = AccountType.PAYMENT,
                    source = Source.POPUP,
                    entryPoint = EntryPoint.ACCOUNT_ENTRY,
                ),
                expectedId = "[Visa CDD/EDD] Provide Information Clicked",
                expectedParams = mapOf(
                    "account_type" to "payment",
                    "kyc_status" to "in_progress",
                    "source" to "popup",
                    "entry_point" to "account_entry",
                ),
            ),
            EventModel(
                event = ProvideInformationClicked(
                    accountType = AccountType.MONEY,
                    source = Source.ACCOUNT_BLOCK,
                    entryPoint = EntryPoint.ACCOUNT_BLOCK,
                ),
                expectedId = "[Visa CDD/EDD] Provide Information Clicked",
                expectedParams = mapOf(
                    "account_type" to "money",
                    "kyc_status" to "in_progress",
                    "source" to "account_block",
                    "entry_point" to "account_block",
                ),
            ),
            EventModel(
                event = ContactSupportClicked(accountType = AccountType.PAYMENT, entryPoint = EntryPoint.ACCOUNT_ENTRY),
                expectedId = "[Visa CDD/EDD] Contact Support Clicked",
                expectedParams = mapOf(
                    "account_type" to "payment",
                    "kyc_status" to "in_progress",
                    "source" to "popup",
                    "entry_point" to "account_entry",
                ),
            ),
        )
    }

    @Nested
    inner class ParamValues {

        @ParameterizedTest
        @EnumSource(AccountType::class)
        fun `GIVEN account type WHEN reported THEN value matches the spec`(accountType: AccountType) {
            // Arrange
            val expected = when (accountType) {
                AccountType.PAYMENT -> "payment"
                AccountType.MONEY -> "money"
            }

            // Act
            val actual = accountType.value

            // Assert
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(Placement::class)
        fun `GIVEN placement WHEN reported THEN value matches the spec`(placement: Placement) {
            // Arrange
            val expected = when (placement) {
                Placement.HOME_ROW -> "home_row"
                Placement.ACCOUNT_BLOCK -> "account_block"
            }

            // Act
            val actual = placement.value

            // Assert
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(EntryPoint::class)
        fun `GIVEN entry point WHEN reported THEN value matches the spec`(entryPoint: EntryPoint) {
            // Arrange
            val expected = when (entryPoint) {
                EntryPoint.ACCOUNT_ENTRY -> "account_entry"
                EntryPoint.ACCOUNT_BLOCK -> "account_block"
                EntryPoint.PUSH -> "push"
            }

            // Act
            val actual = entryPoint.value

            // Assert
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(Source::class)
        fun `GIVEN source WHEN reported THEN value matches the spec`(source: Source) {
            // Arrange
            val expected = when (source) {
                Source.POPUP -> "popup"
                Source.ACCOUNT_BLOCK -> "account_block"
            }

            // Act
            val actual = source.value

            // Assert
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(CloseMethod::class)
        fun `GIVEN close method WHEN reported THEN value matches the spec`(closeMethod: CloseMethod) {
            // Arrange
            val expected = when (closeMethod) {
                CloseMethod.CLOSE_BUTTON -> "close_button"
                CloseMethod.SWIPE -> "swipe"
                CloseMethod.BACK -> "back"
                CloseMethod.UNKNOWN -> "unknown"
            }

            // Act
            val actual = closeMethod.value

            // Assert
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(NoticeState::class)
        fun `GIVEN notice state WHEN reported THEN value matches the spec`(noticeState: NoticeState) {
            // Arrange
            val expected = when (noticeState) {
                NoticeState.SHOWN -> "shown"
                NoticeState.NOT_SHOWN -> "not_shown"
            }

            // Act
            val actual = noticeState.value

            // Assert
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NoticeStateOf {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN required flag WHEN notice state resolved THEN it matches the flag`(model: NoticeStateModel) {
            // Act
            val actual = NoticeState.of(isRequired = model.isRequired)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            NoticeStateModel(isRequired = true, expected = NoticeState.SHOWN),
            NoticeStateModel(isRequired = false, expected = NoticeState.NOT_SHOWN),
        )
    }

    data class EventModel(
        val event: TangemPayKycAdditionalInfoAnalyticsEvents,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    )

    data class NoticeStateModel(val isRequired: Boolean, val expected: NoticeState)
}