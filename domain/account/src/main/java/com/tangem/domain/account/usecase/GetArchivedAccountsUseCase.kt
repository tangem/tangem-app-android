package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

typealias ArchivedAccountList = List<ArchivedAccount>

/**
 * Use case for retrieving archived accounts for a specific user wallet
 *
 * @property crudRepository the repository for performing CRUD operations on accounts
 *
[REDACTED_AUTHOR]
 */
class GetArchivedAccountsUseCase(
    private val crudRepository: AccountsCRUDRepository,
) {

    /**
     * Executes the use case to retrieve archived accounts for the given user wallet
     *
     * @param userWalletId the unique identifier of the user wallet
     */
    operator fun invoke(userWalletId: UserWalletId): LceFlow<Throwable, ArchivedAccountList> = channelFlow {
        val archivedAccounts = getArchivedAccounts(userWalletId = userWalletId)

        archivedAccounts
            .onRight { send(it.lceContent()) }
            .onLeft {
                send(lceLoading())

                launch {
                    fetchArchivedAccounts(userWalletId).getOrElse {
                        send(it.lceError())
                    }
                }
            }

        subscribeOnArchivedAccounts(userWalletId)
    }
        .distinctUntilChanged()

    private suspend fun getArchivedAccounts(userWalletId: UserWalletId): Either<Throwable, ArchivedAccountList> {
        return Either.catch {
            crudRepository.getArchivedAccountListSync(userWalletId = userWalletId).getOrElse {
                error("Archived accounts not found for user wallet: $userWalletId")
            }
        }
    }

    private suspend fun fetchArchivedAccounts(userWalletId: UserWalletId): Either<Throwable, Unit> {
        return Either.catch { crudRepository.fetchArchivedAccounts(userWalletId) }
    }

    private suspend fun ProducerScope<Lce<Throwable, ArchivedAccountList>>.subscribeOnArchivedAccounts(
        userWalletId: UserWalletId,
    ) {
        runCatching { crudRepository.getArchivedAccounts(userWalletId) }
            .getOrElse {
                send(it.lceError())
                return
            }
            .distinctUntilChanged()
            .retryWhen { cause, _ ->
                send(cause.lceError())

                delay(timeMillis = 2000)

                true
            }
            .collectLatest { archivedAccounts ->
                send(archivedAccounts.lceContent())
            }
    }
}