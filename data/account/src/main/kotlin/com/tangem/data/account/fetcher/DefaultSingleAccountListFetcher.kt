package com.tangem.data.account.fetcher

import arrow.core.Either
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.domain.account.fetcher.SingleAccountListFetcher

/**
 * Implementation of [SingleAccountListFetcher]
 *
 * @property walletAccountsFetcher instance of [WalletAccountsFetcher] to fetch accounts for a single wallet
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleAccountListFetcher(
    private val walletAccountsFetcher: WalletAccountsFetcher,
) : SingleAccountListFetcher {

    override suspend fun invoke(params: SingleAccountListFetcher.Params): Either<Throwable, Unit> = Either.catch {
        walletAccountsFetcher.fetch(userWalletId = params.userWalletId)
    }
}