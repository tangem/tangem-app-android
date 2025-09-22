package com.tangem.data.account.fetcher

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.fetcher.MultiAccountListFetcher
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of [MultiAccountListFetcher]
 *
 * @property singleAccountListFetcher instance of [SingleAccountListFetcher] to fetch accounts for a single wallet
 * @property userWalletsStore         instance of [UserWalletsStore] to get all user wallets
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiAccountListFetcher(
    private val singleAccountListFetcher: SingleAccountListFetcher,
    private val userWalletsStore: UserWalletsStore,
) : MultiAccountListFetcher {

    override suspend fun invoke(params: MultiAccountListFetcher.Params): Either<Throwable, Unit> = either {
        when (params) {
            is MultiAccountListFetcher.Params.Set -> {
                if (params.ids.isEmpty()) {
                    Timber.d("No wallet ids provided to fetch accounts.")
                    return@either
                }

                val errors = ConcurrentHashMap<UserWalletId, Throwable>()

                coroutineScope {
                    params.ids.forEach { userWalletId ->
                        launch {
                            singleAccountListFetcher(
                                params = SingleAccountListFetcher.Params(userWalletId),
                            )
                                .onLeft { error -> errors[userWalletId] = error }
                        }
                    }
                }

                if (errors.isNotEmpty()) {
                    val exception = IllegalStateException(
                        "Failed to fetch accounts for wallets:\n${errors.entries.joinToString(separator = "\n")}",
                    )

                    Timber.e(exception)

                    raise(exception)
                }
            }
            MultiAccountListFetcher.Params.All -> {
                val userWalletsIds = userWalletsStore.userWalletsSync.map(UserWallet::walletId).toSet()

                invoke(params = MultiAccountListFetcher.Params.Set(ids = userWalletsIds)).bind()
            }
        }
    }
}