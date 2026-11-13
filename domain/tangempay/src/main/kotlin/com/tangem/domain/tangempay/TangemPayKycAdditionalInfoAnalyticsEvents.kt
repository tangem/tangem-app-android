package com.tangem.domain.tangempay

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class TangemPayKycAdditionalInfoAnalyticsEvents(
    event: String,
    params: Map<String, String>,
) : AnalyticsEvent(category = "Visa CDD/EDD", event = event, params = params) {

    class NoticeShown(accountType: AccountType, placement: Placement) : TangemPayKycAdditionalInfoAnalyticsEvents(
        event = "Notice Shown",
        params = commonParams(accountType) + ("placement" to placement.value),
    )

    class PopupShown(accountType: AccountType, entryPoint: EntryPoint) : TangemPayKycAdditionalInfoAnalyticsEvents(
        event = "Popup Shown",
        params = commonParams(accountType) + ("entry_point" to entryPoint.value),
    )

    class PopupClosed(
        accountType: AccountType,
        entryPoint: EntryPoint,
        closeMethod: CloseMethod,
    ) : TangemPayKycAdditionalInfoAnalyticsEvents(
        event = "Popup Closed",
        params = commonParams(accountType) + mapOf(
            "entry_point" to entryPoint.value,
            "close_method" to closeMethod.value,
        ),
    )

    class ProvideInformationClicked(
        accountType: AccountType,
        source: Source,
        entryPoint: EntryPoint,
    ) : TangemPayKycAdditionalInfoAnalyticsEvents(
        event = "Provide Information Clicked",
        params = commonParams(accountType) + mapOf(
            "source" to source.value,
            "entry_point" to entryPoint.value,
        ),
    )

    class ContactSupportClicked(
        accountType: AccountType,
        entryPoint: EntryPoint,
    ) : TangemPayKycAdditionalInfoAnalyticsEvents(
        event = "Contact Support Clicked",
        params = commonParams(accountType) + mapOf(
            "source" to Source.POPUP.value,
            "entry_point" to entryPoint.value,
        ),
    )

    enum class AccountType(val value: String) {
        PAYMENT("payment"),
        MONEY("money"),
    }

    enum class Placement(val value: String) {
        HOME_ROW("home_row"),
        ACCOUNT_BLOCK("account_block"),
    }

    enum class EntryPoint(val value: String) {
        ACCOUNT_ENTRY("account_entry"),
        ACCOUNT_BLOCK("account_block"),
    }

    enum class Source(val value: String) {
        POPUP("popup"),
        ACCOUNT_BLOCK("account_block"),
    }

    enum class CloseMethod(val value: String) {
        CLOSE_BUTTON("close_button"),
        SWIPE("swipe"),
        BACK("back"),
        UNKNOWN("unknown"),
    }

    enum class NoticeState(val value: String) {
        SHOWN("shown"),
        NOT_SHOWN("not_shown"),
        ;

        companion object {
            fun of(isEnabled: Boolean, isRequired: Boolean): NoticeState? = when {
                !isEnabled -> null
                isRequired -> SHOWN
                else -> NOT_SHOWN
            }
        }
    }

    private companion object {
        const val KYC_STATUS_IN_PROGRESS = "in_progress"

        fun commonParams(accountType: AccountType): Map<String, String> = mapOf(
            "account_type" to accountType.value,
            "kyc_status" to KYC_STATUS_IN_PROGRESS,
        )
    }
}