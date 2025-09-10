package com.tangem.data.account.converter

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

internal fun createWalletAccountDTO(
    userWalletId: UserWalletId,
    accountId: String? = null,
    accountName: String? = null,
    icon: String? = null,
    iconColor: String? = null,
    derivationIndex: Int? = null,
    tokens: List<UserTokensResponse.Token>? = emptyList(),
): WalletAccountDTO {
    val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId = userWalletId)

    return WalletAccountDTO(
        id = accountId ?: mainAccount.accountId.value,
        name = accountName ?: (mainAccount.accountName as? AccountName.Custom)?.value,
        derivationIndex = derivationIndex ?: mainAccount.derivationIndex.value,
        icon = icon ?: mainAccount.icon.value.name,
        iconColor = iconColor ?: mainAccount.icon.color.name,
        tokens = tokens,
    )
}

internal fun createCryptoPortfolio(userWalletId: UserWalletId): Account.CryptoPortfolio {
    return Account.CryptoPortfolio.createMainAccount(userWalletId = userWalletId)
}

internal fun createGetWalletAccountsResponse(
    userWalletId: UserWalletId,
    groupType: UserTokensResponse.GroupType = UserTokensResponse.GroupType.NETWORK,
    sortType: UserTokensResponse.SortType = UserTokensResponse.SortType.BALANCE,
    accountId: String? = null,
    accountName: String? = null,
    icon: String? = null,
    iconColor: String? = null,
    derivationIndex: Int? = null,
    tokens: List<UserTokensResponse.Token>? = emptyList(),
): GetWalletAccountsResponse {
    return GetWalletAccountsResponse(
        wallet = GetWalletAccountsResponse.Wallet(
            version = 0,
            group = groupType,
            sort = sortType,
            totalAccounts = 1,
        ),
        accounts = buildList {
            createWalletAccountDTO(
                userWalletId = userWalletId,
                accountId = accountId,
                accountName = accountName,
                icon = icon,
                iconColor = iconColor,
                derivationIndex = derivationIndex,
                tokens = tokens,
            )
                .let(::add)
        },
        unassignedTokens = emptyList(),
    )
}

internal fun createAccountList(
    userWallet: UserWallet,
    sortType: TokensSortType = TokensSortType.BALANCE,
    groupType: TokensGroupType = TokensGroupType.NETWORK,
): AccountList {
    return AccountList(
        userWallet = userWallet,
        accounts = setOf(createCryptoPortfolio(userWallet.walletId)),
        totalAccounts = 1,
        sortType = sortType,
        groupType = groupType,
    )
        .getOrNull()!!
}