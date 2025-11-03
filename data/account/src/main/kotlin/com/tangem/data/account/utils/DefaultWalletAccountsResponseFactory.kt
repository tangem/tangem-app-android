package com.tangem.data.account.utils

import com.tangem.data.account.converter.CryptoPortfolioConverter
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/**
 * Factory to create default [GetWalletAccountsResponse].
 *
 * @property userWalletsListRepository repository to get user wallet information
 * @property cryptoPortfolioCF         converter factory to convert crypto portfolio accounts
 * @property userTokensResponseFactory factory to create [UserTokensResponse]
 * @property cardCryptoCurrencyFactory factory to get default coins for multi-currency wallet
 *
[REDACTED_AUTHOR]
 */
internal class DefaultWalletAccountsResponseFactory @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val cryptoPortfolioCF: CryptoPortfolioConverter.Factory,
    private val userTokensResponseFactory: UserTokensResponseFactory,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
) {

    suspend fun create(userWalletId: UserWalletId, userTokensResponse: UserTokensResponse?): GetWalletAccountsResponse {
        val userWallet = userWalletsListRepository.userWalletsSync().firstOrNull { it.walletId == userWalletId }

        val accountDTOs = userWallet?.let(::createDefaultAccountDTOs).orEmpty()
        val response = userTokensResponse.orDefault(userWallet = userWallet)

        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = response.group,
                sort = response.sort,
                totalAccounts = accountDTOs.size,
            ),
            accounts = accountDTOs.assignTokens(userWalletId = userWalletId, tokens = response.tokens),
            unassignedTokens = emptyList(),
        )
    }

    private fun createDefaultAccountDTOs(userWallet: UserWallet): List<WalletAccountDTO> {
        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()

        val converter = cryptoPortfolioCF.create(userWallet = userWallet)

        return converter.convertListBack(input = accounts)
    }

    private fun UserTokensResponse?.orDefault(userWallet: UserWallet?): UserTokensResponse {
        if (this != null) return this

        return userTokensResponseFactory.createUserTokensResponse(
            currencies = userWallet?.let(cardCryptoCurrencyFactory::createDefaultCoinsForMultiCurrencyWallet).orEmpty(),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )
    }
}