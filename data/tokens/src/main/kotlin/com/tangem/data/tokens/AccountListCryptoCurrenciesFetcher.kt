package com.tangem.data.tokens

import arrow.core.Either
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.wallet.UserWalletId
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
 * @property singleJointAccountListFetcher fetcher of the joint accounts the wallet participates in
 * @property jointAccountFeatureToggles joint account feature toggles
 * @property dispatchers provider for coroutine dispatchers used to run fetch operations
 *
[REDACTED_AUTHOR]
 */
internal class AccountListCryptoCurrenciesFetcher(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val singleJointAccountListFetcher: SingleJointAccountListFetcher,
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiWalletAccountListFetcher {

    override suspend fun invoke(params: Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            val userWallet = userWalletsListRepository.getSyncStrict(id = params.userWalletId)

            if (!userWallet.isMultiCurrency) error("${this::class.simpleName} supports only multi-currency wallet")

            coroutineScope {
                // Independent of the accounts, so they load side by side; and they never fail the accounts — the
                // balance path copes with their absence
                if (jointAccountFeatureToggles.isJointAccountEnabled) {
                    launch { fetchJointAccounts(userWalletId = params.userWalletId) }
                }

                walletAccountsFetcher.fetch(userWalletId = params.userWalletId)
            }
        }
    }

    private suspend fun fetchJointAccounts(userWalletId: UserWalletId) {
        singleJointAccountListFetcher(SingleJointAccountListFetcher.Params(userWalletId = userWalletId))
            .onLeft { TangemLogger.e("Failed to fetch the joint accounts of $userWalletId", it) }
    }
}