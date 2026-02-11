package com.tangem.features.account.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
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

    class ButtonManageTokens(
        accountDerivation: Int?,
    ) : AccountSettingsAnalyticEvents(
        event = "Button - Manage Tokens",
        params = buildMap {
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
        },
    )

    class ButtonArchiveAccount(
        accountDerivation: Int?,
    ) : AccountSettingsAnalyticEvents(
        event = "Button - Archive Account",
        params = buildMap {
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
        },
    )

    class ButtonArchiveAccountConfirmation(
        accountDerivation: Int?,
    ) : AccountSettingsAnalyticEvents(
        event = "Button - Archive Account Confirmation",
        params = buildMap {
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
        },
    )

    class ButtonCancelAccountArchivation(
        accountDerivation: Int?,
    ) : AccountSettingsAnalyticEvents(
        event = "Button - Cancel Account Archivation",
        params = buildMap {
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
        },
    )

    class AccountArchived : AccountSettingsAnalyticEvents(
        event = "Account Archived",
    )

    class ButtonEdit : AccountSettingsAnalyticEvents(
        event = "Button - Edit",
    )

    class AccountEditScreenOpened(
        accountDerivation: Int?,
    ) : AccountSettingsAnalyticEvents(
        event = "Account Edit Screen Opened",
        params = buildMap {
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
        },
    )

    class ButtonSave(
        val name: AccountName,
        val icon: CryptoPortfolioIcon,
        accountDerivation: Int?,
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
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
        },
    )

    class ButtonAddNewAccount(
        val name: AccountName,
        val icon: CryptoPortfolioIcon,
        accountDerivation: Int?,
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
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
        },
    )

    class AccountError(
        val source: Source,
        val error: String,
        accountDerivation: Int?,
    ) : AccountSettingsAnalyticEvents(
        event = "Account Error",
        params = buildMap {
            put("Error", error)
            put("Source", source.value)
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
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