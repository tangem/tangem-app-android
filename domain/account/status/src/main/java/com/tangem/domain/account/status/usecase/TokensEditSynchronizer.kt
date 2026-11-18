package com.tangem.domain.account.status.usecase

import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/** What the user asked for, kept to be repeated on a list refreshed from the backend */
internal data class TokensEdit(
    val accountId: AccountId,
    val add: List<CryptoCurrency>,
    val remove: List<CryptoCurrency>,
)

/**
 * Pushes token edits to the backend. Syncs of one wallet run one at a time, because a refused one replaces the
 * whole local list with the backend's one — along with every edit that is saved locally but not pushed yet. Those
 * edits are therefore kept from the moment they are saved, in the order they were made, and are all repeated on
 * the refreshed list.
 *
 * @param applyEdit makes the edit on the account's currencies
 */
internal class TokensEditSynchronizer(
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val applyEdit: (currencies: List<CryptoCurrency>, edit: TokensEdit) -> ModifiedCurrencyList,
) {

    private val pendingEditsByWallet = ConcurrentHashMap<UserWalletId, List<TokensEdit>>()
    private val syncMutexes = ConcurrentHashMap<UserWalletId, Mutex>()

    /** Called before the edited account is saved, so a refresh that lands in between still repeats the edit */
    fun register(edit: TokensEdit) {
        pendingEditsByWallet.compute(edit.accountId.userWalletId) { _, edits -> edits.orEmpty() + edit }
    }

    /** By identity: two equal edits in a row are still two edits */
    fun unregister(edit: TokensEdit) {
        pendingEditsByWallet.computeIfPresent(edit.accountId.userWalletId) { _, edits ->
            edits.filterNot { it === edit }.ifEmpty { null }
        }
    }

    suspend fun sync(edit: TokensEdit) {
        val userWalletId = edit.accountId.userWalletId

        syncMutexes.computeIfAbsent(userWalletId) { Mutex() }.withLock {
            runSuspendCatching {
                val isRefreshed = accountsCRUDRepository.syncTokensOrRefresh(userWalletId)
                if (isRefreshed) repeatPendingOnRefreshedList(edit)
            }
                .onFailure { TangemLogger.e("Failed to sync tokens for wallet $userWalletId", it) }

            unregister(edit)
        }
    }

    /**
     * Another device changed the list first: the wallet now holds the backend's tokens, and every pending edit is
     * made on them once more. A second refusal drops this edit; the later ones are restored locally and go out with
     * their own syncs.
     */
    private suspend fun repeatPendingOnRefreshedList(edit: TokensEdit) {
        val userWalletId = edit.accountId.userWalletId
        if (!applyPendingEdits(userWalletId)) return

        val isRefusedAgain = accountsCRUDRepository.syncTokensOrRefresh(userWalletId)
        if (!isRefusedAgain) return

        TangemLogger.e("Tokens edit of ${edit.accountId} is dropped: refused as stale twice")
        unregister(edit)
        applyPendingEdits(userWalletId)
    }

    /** @return `true` when at least one account was changed */
    private suspend fun applyPendingEdits(userWalletId: UserWalletId): Boolean {
        var hasChanges = false

        pendingEditsByWallet[userWalletId].orEmpty().groupBy(TokensEdit::accountId).forEach { (accountId, edits) ->
            val account = accountsCRUDRepository.getAccountSync(accountId).getOrNull() ?: return@forEach
            var currencies = account.cryptoCurrencies
            var isAccountChanged = false

            edits.forEach { pendingEdit ->
                val modifiedCurrencyList = applyEdit(currencies, pendingEdit)

                currencies = modifiedCurrencyList.total
                isAccountChanged = isAccountChanged || modifiedCurrencyList.hasChanges
            }

            if (isAccountChanged) {
                accountsCRUDRepository.saveAccount(account.copySealed(cryptoCurrencies = currencies))
                hasChanges = true
            }
        }

        return hasChanges
    }
}