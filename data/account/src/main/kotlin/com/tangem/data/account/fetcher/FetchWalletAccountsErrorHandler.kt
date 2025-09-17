package com.tangem.data.account.fetcher

import com.tangem.data.account.converter.CryptoPortfolioConverter
import com.tangem.data.account.utils.assignTokens
import com.tangem.data.account.utils.toUserTokensResponse
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import timber.log.Timber
import javax.inject.Inject

/**
 * Handles errors that occur during the fetching of wallet accounts
 *
 * @property userTokensSaver           saves user tokens to the storage
 * @property userWalletsStore          provides access to user wallet data
 * @property userTokensResponseStore   provides access to user token responses.
 * @property cryptoPortfolioCF         factory for converting crypto portfolios
 * @property userTokensResponseFactory factory for creating user token responses
 * @property cardCryptoCurrencyFactory factory for creating default cryptocurrencies for multi-currency wallets
 *
 * @see DefaultWalletAccountsFetcher
 *
[REDACTED_AUTHOR]
 */
internal class FetchWalletAccountsErrorHandler @Inject constructor(
    private val userTokensSaver: UserTokensSaver,
    private val userWalletsStore: UserWalletsStore,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val cryptoPortfolioCF: CryptoPortfolioConverter.Factory,
    private val userTokensResponseFactory: UserTokensResponseFactory,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
) {

    /**
     * Handles the error that occurred during the fetching of wallet accounts.
     * [pushWalletAccounts] and [storeWalletAccounts] are functions that passed as parameters to avoid
     * cyclic dependencies.
     *
     * @param error                 the error that occurred
     * @param userWalletId          the ID of the user wallet
     * @param savedAccountsResponse the previously saved wallet accounts response, if available
     * @param pushWalletAccounts    function to push wallet accounts to the server
     * @param storeWalletAccounts   function to store wallet accounts locally
     */
    suspend fun handle(
        error: ApiResponseError,
        userWalletId: UserWalletId,
        savedAccountsResponse: GetWalletAccountsResponse?,
        pushWalletAccounts: suspend (userWalletId: UserWalletId, accounts: List<WalletAccountDTO>) -> Unit,
        storeWalletAccounts: suspend (userWalletId: UserWalletId, response: GetWalletAccountsResponse) -> Unit,
    ) {
        val isResponseUpToDate = error.isNetworkError(code = Code.NOT_MODIFIED)
        if (isResponseUpToDate) {
            Timber.e("ETag is up to date, no need to update accounts for wallet: $userWalletId")
            return
        }

        val (accountDTOs, userTokensResponse) = if (savedAccountsResponse == null) {
            val userWallet = userWalletsStore.getSyncStrict(key = userWalletId)

            createDefaultAccountDTOs(userWallet) to getFromLegacyStore(userWalletId).orDefault(userWallet)
        } else {
            savedAccountsResponse.accounts to savedAccountsResponse.toUserTokensResponse()
        }

        val isNotFoundError = error.isNetworkError(code = Code.NOT_FOUND)
        if (isNotFoundError) {
            pushWalletAccounts(userWalletId, accountDTOs)
            userTokensSaver.push(userWalletId = userWalletId, response = userTokensResponse)
        }

        val response = savedAccountsResponse.orDefault(userWalletId, accountDTOs, userTokensResponse)
        storeWalletAccounts(userWalletId, response)
    }

    private fun createDefaultAccountDTOs(userWallet: UserWallet): List<WalletAccountDTO> {
        val accounts = AccountList.empty(userWallet).accounts
            .filterIsInstance<Account.CryptoPortfolio>()

        val converter = cryptoPortfolioCF.create(userWallet = userWallet)

        return converter.convertListBack(input = accounts)
    }

    private suspend fun getFromLegacyStore(userWalletId: UserWalletId): UserTokensResponse? {
        return userTokensResponseStore.getSyncOrNull(userWalletId)
            .also { userTokensResponseStore.clear(userWalletId) }
    }

    private fun UserTokensResponse?.orDefault(userWallet: UserWallet): UserTokensResponse {
        if (this != null) return this

        return userTokensResponseFactory.createUserTokensResponse(
            currencies = cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(userWallet = userWallet),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )
    }

    private fun GetWalletAccountsResponse?.orDefault(
        userWalletId: UserWalletId,
        accountDTOs: List<WalletAccountDTO>,
        userTokensResponse: UserTokensResponse,
    ): GetWalletAccountsResponse {
        if (this != null) return this

        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = userTokensResponse.group,
                sort = userTokensResponse.sort,
                totalAccounts = accountDTOs.size,
            ),
            accounts = accountDTOs.assignTokens(userWalletId = userWalletId, tokens = userTokensResponse.tokens),
            unassignedTokens = emptyList(),
        )
    }
}