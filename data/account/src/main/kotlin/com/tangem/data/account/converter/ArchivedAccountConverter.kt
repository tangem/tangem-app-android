package com.tangem.data.account.converter

import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.converter.Converter

/**
 * Converts a [WalletAccountDTO] to an [ArchivedAccount]
 *
 * @param userWalletId the ID of the user wallet associated with the account
 *
[REDACTED_AUTHOR]
 */
internal class ArchivedAccountConverter(
    private val userWalletId: UserWalletId,
) : Converter<WalletAccountDTO, ArchivedAccount> {

    override fun convert(value: WalletAccountDTO): ArchivedAccount {
        return ArchivedAccount(
            accountId = value.id.toAccountId(userWalletId = userWalletId),
            name = value.name.toAccountName(),
            icon = value.toIcon(),
            derivationIndex = value.derivationIndex.toDerivationIndex(),
            tokensCount = value.totalTokens ?: error("Total tokens should not be null"),
            networksCount = value.totalNetworks ?: error("Total networks should not be null"),
        )
    }
}