package com.tangem.features.account.archived.entity

import com.tangem.common.ui.account.toUM
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.usecase.ArchivedAccountList
import com.tangem.domain.models.account.AccountId
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber
import javax.inject.Inject

internal class AccountArchivedUMBuilder @Inject constructor() {

    fun mapContent(
        accounts: ArchivedAccountList,
        onCloseClick: () -> Unit,
        onRecoverClick: (account: ArchivedAccount) -> Unit,
    ) = AccountArchivedUM.Content(
        onCloseClick = onCloseClick,
        accounts = accounts
            .map { account -> account.mapArchivedAccountUM(onRecoverClick) }
            .toImmutableList(),
    )

    private fun ArchivedAccount.mapArchivedAccountUM(onRecoverClick: (account: ArchivedAccount) -> Unit) =
        ArchivedAccountUM(
            accountId = accountId.value,
            accountName = name.toUM().value,
            accountIconUM = icon.toUM(),
            isLoading = false,
            tokensInfo = pluralReference(
                R.plurals.common_tokens_count,
                count = tokensCount,
                formatArgs = wrappedList(tokensCount),
            ),
            networksInfo = pluralReference(
                R.plurals.common_networks_count,
                count = networksCount,
                formatArgs = wrappedList(networksCount),
            ),
            onClick = { onRecoverClick(this) },
        )

    fun mapError(
        throwable: Throwable,
        onCloseClick: () -> Unit,
        getArchivedAccounts: () -> Unit,
    ): AccountArchivedUM.Error {
        Timber.e(throwable)
        return AccountArchivedUM.Error(
            onCloseClick = onCloseClick,
            onRetryClick = { getArchivedAccounts() },
        )
    }

    companion object {

        fun AccountArchivedUM.toggleProgress(accountId: AccountId, isLoading: Boolean): AccountArchivedUM {
            return when (this) {
                is AccountArchivedUM.Error,
                is AccountArchivedUM.Loading,
                -> this

                is AccountArchivedUM.Content -> copy(
                    accounts = accounts.map { accountUM ->
                        if (accountUM.accountId == accountId.value) {
                            accountUM.copy(isLoading = isLoading)
                        } else {
                            accountUM
                        }
                    }.toImmutableList(),
                )
            }
        }
    }
}