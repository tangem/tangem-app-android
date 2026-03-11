package com.tangem.data.tokens

import arrow.core.Either
import arrow.core.right
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.datasource.api.tangemTech.models.account.flattenTokens
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher.Params
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * Implementation of [MultiWalletCryptoCurrenciesFetcher] that fetches crypto currencies of all accounts
 *
 * @property userWalletsListRepository repository to get user wallets
 * @property walletAccountsFetcher instance of [WalletAccountsFetcher] to fetch accounts for a multi wallet
 * @property expressServiceFetcher fetcher of express service
 * @property dispatchers           dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class AccountListCryptoCurrenciesFetcher(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val expressServiceFetcher: ExpressServiceFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiWalletCryptoCurrenciesFetcher {

    override suspend fun invoke(params: Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            val userWallet = userWalletsListRepository.getSyncStrict(id = params.userWalletId)

            if (!userWallet.isMultiCurrency) error("${this::class.simpleName} supports only multi-currency wallet")

            val response = walletAccountsFetcher.fetch(userWalletId = params.userWalletId)

            expressServiceFetcher.fetch(
                userWallet = userWallet,
                assetIds = response.flattenTokens().mapTo(hashSetOf()) {
                    ExpressAsset.ID(networkId = it.networkId, contractAddress = it.contractAddress)
                },
            )

            Unit.right()
        }
    }
}