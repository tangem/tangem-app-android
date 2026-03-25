package com.tangem.feature.wallet.presentation.wallet.loaders

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newSingleThreadContext
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Base wallet screen content loader. Use it to load content by [UserWallet].
 *
 * @property factory factory that creates loader
 * @property storage storage that save loader's jobs
 *
[REDACTED_AUTHOR]
 */
@ModelScoped
internal class WalletScreenContentLoader @Inject constructor(
    private val factory: WalletContentLoaderFactory,
    private val storage: WalletLoaderStorage,
) {

    private val singleBackgroundDispatcher: CloseableCoroutineDispatcher =
        newSingleThreadContext(name = "Background Main")

    /**
     * Load content by [UserWallet]
     *
     * @param userWallet     user wallet
     * @param isRefresh      flag that determinate if content must load again
     * @param coroutineScope coroutine scope
     */
    fun load(userWallet: UserWallet, isRefresh: Boolean = false, coroutineScope: CoroutineScope) {
        if (userWallet.isLocked) return

        val id = userWallet.walletId
        if (!storage.contains(id)) {
            loadInternal(userWallet, coroutineScope, isRefresh)
        } else {
            if (isRefresh) {
                storage.remove(id)
                loadInternal(userWallet, coroutineScope, isRefresh = true)
            } else {
                TangemLogger.d("$id content loading has already started")
            }
        }
    }

    /** Cancel loading by [id] */
    fun cancel(id: UserWalletId) {
        TangemLogger.d("$id content loading is canceled")
        storage.remove(id)
    }

    fun cancelAll() {
        TangemLogger.d("All content loading is canceled")
        storage.clear()
        singleBackgroundDispatcher.close()
    }

    private fun loadInternal(userWallet: UserWallet, coroutineScope: CoroutineScope, isRefresh: Boolean) {
        val loader = factory.create(
            userWallet = userWallet,
            isRefresh = isRefresh,
        )

        if (loader == null) {
            TangemLogger.e("Impossible to create loader for $userWallet")
            return
        }

        TangemLogger.d("${userWallet.walletId} content loading is ${if (isRefresh) "re" else ""}started")

        loader.subscribers
            .map { it.subscribe(coroutineScope, singleBackgroundDispatcher) }
            .let { storage.set(userWallet.walletId, it) }
    }
}