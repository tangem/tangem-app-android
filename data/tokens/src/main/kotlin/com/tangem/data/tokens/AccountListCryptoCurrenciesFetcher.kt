package com.tangem.data.tokens

import arrow.core.Either
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.MultiWalletAccountListFetcher
import com.tangem.domain.tokens.MultiWalletAccountListFetcher.Params
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * Implementation of [MultiWalletAccountListFetcher] that fetches the account list for a multi-currency wallet
 * by delegating to [WalletAccountsFetcher].
 *
 * @property userWalletsListRepository repository to get user wallets
 * @property walletAccountsFetcher instance of [WalletAccountsFetcher] to fetch accounts for a multi wallet
 * @property dispatchers provider for coroutine dispatchers used to run fetch operations
 *
[REDACTED_AUTHOR]
 */
internal class AccountListCryptoCurrenciesFetcher(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiWalletAccountListFetcher {

    override suspend fun invoke(params: Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            val userWallet = userWalletsListRepository.getSyncStrict(id = params.userWalletId)

            if (!userWallet.isMultiCurrency) error("${this::class.simpleName} supports only multi-currency wallet")

            walletAccountsFetcher.fetch(userWalletId = params.userWalletId)
        }
    }
}