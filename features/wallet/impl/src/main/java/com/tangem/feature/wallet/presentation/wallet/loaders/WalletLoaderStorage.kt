package com.tangem.feature.wallet.presentation.wallet.loaders

import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WalletLoaderStorage @Inject constructor() {

    private val loaders = ConcurrentHashMap<UserWalletId, List<Job>>()

    fun contains(id: UserWalletId) = loaders.containsKey(id)

    fun set(id: UserWalletId, jobs: List<Job>) {
        loaders[id] = jobs
    }

    fun remove(id: UserWalletId) {
        loaders[id]?.let {
            it.forEach(Job::cancel)
            loaders.remove(id)
        }
    }

    fun clear() {
        loaders.values.forEach { it.forEach(Job::cancel) }
        loaders.clear()
    }
}