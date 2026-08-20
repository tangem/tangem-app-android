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
            accounts = value.accounts.mapNotNull { account ->
                when (account) {
                    is Account.CryptoPortfolio -> account.toDTO()
                    is Account.Joint -> account.toDTO()
                    else -> null
                }
            },
        )
    }

    private fun Account.CryptoPortfolio.toDTO(): SaveWalletAccountsResponse.AccountDTO {
        return SaveWalletAccountsResponse.AccountDTO(
            id = accountId.value,
            name = AccountNameConverter.convert(value = accountName),
            derivationIndex = derivationIndex.value,
            icon = icon.value.name,
            iconColor = icon.color.name,
            type = WalletAccountDTO.TYPE_CRYPTO,
        )
    }

    private fun Account.Joint.toDTO(): SaveWalletAccountsResponse.AccountDTO {
        return SaveWalletAccountsResponse.AccountDTO(
            id = accountId.value,
            name = AccountNameConverter.convert(value = accountName),
            derivationIndex = derivationIndex.value,
            icon = icon.value.name,
            iconColor = icon.color.name,
            type = WalletAccountDTO.TYPE_JOINT,
        )
    }
}