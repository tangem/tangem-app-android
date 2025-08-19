package com.tangem.data.account.converter

import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.utils.converter.Converter

/**
 * Converts an [AccountList] to a [SaveWalletAccountsResponse]
 *
[REDACTED_AUTHOR]
 */
internal object SaveWalletAccountsResponseConverter : Converter<AccountList, SaveWalletAccountsResponse> {

    override fun convert(value: AccountList): SaveWalletAccountsResponse {
        return SaveWalletAccountsResponse(
            accounts = value.accounts
                .filterIsInstance<Account.CryptoPortfolio>()
                .map(::toDTO),
        )
    }

    private fun toDTO(account: Account.CryptoPortfolio): WalletAccountDTO {
        return WalletAccountDTO(
            id = account.accountId.value,
            name = account.accountName.value,
            derivationIndex = account.derivationIndex.value,
            icon = account.icon.value.name,
            iconColor = account.icon.color.name,
        )
    }
}