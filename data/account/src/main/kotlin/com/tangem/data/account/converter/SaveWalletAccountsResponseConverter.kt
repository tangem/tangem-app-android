package com.tangem.data.account.converter

import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
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
                    is Account.CryptoPortfolio -> account.toDTO(
                        icon = account.icon,
                        derivationIndex = account.derivationIndex,
                        type = WalletAccountDTO.Type.CRYPTO,
                    )
                    is Account.Joint -> account.toDTO(
                        icon = account.icon,
                        derivationIndex = account.derivationIndex,
                        type = WalletAccountDTO.Type.JOINT,
                    )
                    // The special accounts are the app's own: the backend neither stores nor counts them
                    else -> null
                }
            },
        )
    }

    /**
     * The two account kinds the backend stores differ only in their [type] — the icon and the derivation index are
     * passed in because they live on the concrete account rather than on [Account].
     */
    private fun Account.toDTO(
        icon: CryptoPortfolioIcon,
        derivationIndex: DerivationIndex,
        type: WalletAccountDTO.Type,
    ): SaveWalletAccountsResponse.AccountDTO {
        return SaveWalletAccountsResponse.AccountDTO(
            id = accountId.value,
            name = AccountNameConverter.convert(value = accountName),
            derivationIndex = derivationIndex.value,
            icon = icon.value.name,
            iconColor = icon.color.name,
            type = type.value,
        )
    }
}