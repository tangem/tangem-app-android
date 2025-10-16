package com.tangem.data.tokens

import arrow.core.Either
import arrow.core.right
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher.Params
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * Implementation of [MultiWalletCryptoCurrenciesFetcher] that fetches crypto currencies of all accounts
 *
 * @property userWalletsStore      [UserWallet]'s store
 * @property walletAccountsFetcher instance of [WalletAccountsFetcher] to fetch accounts for a multi wallet
 * @property dispatchers           dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class AccountListCryptoCurrenciesFetcher(
    private val userWalletsStore: UserWalletsStore,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiWalletCryptoCurrenciesFetcher {

    override suspend fun invoke(params: Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            val userWallet = userWalletsStore.getSyncStrict(key = params.userWalletId)

            if (!userWallet.isMultiCurrency) error("${this::class.simpleName} supports only multi-currency wallet")

            walletAccountsFetcher.fetch(userWalletId = params.userWalletId)

            Unit.right()
        }
    }
}