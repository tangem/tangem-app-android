package com.tangem.features.account.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.account.AccountCreateEditComponent

sealed class AccountSettingsAnalyticEvents(
    category: String = "Settings / Account",
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    class AccountSettingsScreenOpened : AccountSettingsAnalyticEvents(
        event = "Account Settings Screen Opened",
    )

    class ButtonManageTokens : AccountSettingsAnalyticEvents(
        event = "Button - Manage Tokens",
    )

    class ButtonArchiveAccount : AccountSettingsAnalyticEvents(
        event = "Button - Archive Account",
    )

    class ButtonArchiveAccountConfirmation : AccountSettingsAnalyticEvents(
        event = "Button - Archive Account Confirmation",
    )

    class ButtonCancelAccountArchivation : AccountSettingsAnalyticEvents(
        event = "Button - Cancel Account Archivation",
    )

    class AccountArchived : AccountSettingsAnalyticEvents(
        event = "Account Archived",
    )

    class ButtonEdit : AccountSettingsAnalyticEvents(
        event = "Button - Edit",
    )

    class AccountEditScreenOpened : AccountSettingsAnalyticEvents(
        event = "Account Edit Screen Opened",
    )

    class ButtonSave(
        val name: AccountName,
        val icon: CryptoPortfolioIcon,
    ) : AccountSettingsAnalyticEvents(
        event = "Button - Save",
        params = buildMap {
            val accountName = when (name) {
                is AccountName.Custom -> name.value
                AccountName.DefaultMain -> "DefaultMain"
            }
            put("Name", accountName)
            put("Color", icon.color.name)
            put("Icon", icon.value.name)
        },
    )

    class ButtonAddNewAccount(
        val name: AccountName,
        val icon: CryptoPortfolioIcon,
        val derivationIndex: Int,
    ) : AccountSettingsAnalyticEvents(
        event = "Button - Add New Account",
        params = buildMap {
            val accountName = when (name) {
                is AccountName.Custom -> name.value
                AccountName.DefaultMain -> "DefaultMain"
            }
            put("Name", accountName)
            put("Color", icon.color.name)
            put("Icon", icon.value.name)
            put("Derivation", derivationIndex.toString())
        },
    )

    class AccountError(
        val source: Source,
        val error: String,
    ) : AccountSettingsAnalyticEvents(
        event = "Account Error",
        params = buildMap {
            put("Error", error)
        },
    )

    enum class Source(val value: String) {
        NEW_ACCOUNT("New Account"), EDIT("Edit"), ARCHIVE("Archive")
    }

    companion object {
        fun AccountCreateEditComponent.Params.toAnalyticSource() = when (this) {
            is AccountCreateEditComponent.Params.Create -> Source.NEW_ACCOUNT
            is AccountCreateEditComponent.Params.Edit -> Source.EDIT
        }
    }
}