package com.tangem.datasource.exchangeservice.swap

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.request.AssetsRequestBody
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.datasource.local.token.ExpressAssetsStore
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

typealias InitializationStatusFlow = MutableStateFlow<Lce<Throwable, List<Asset>>>

/**
 * Default implementation of [ExpressServiceLoader]
 *
 * @property tangemExpressApi   express api
 * @property expressAssetsStore local storage
 *
[REDACTED_AUTHOR]
 */
internal class DefaultExpressServiceLoader @Inject constructor(
    private val tangemExpressApi: TangemExpressApi,
    private val expressAssetsStore: ExpressAssetsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : ExpressServiceLoader {

    private val initializationStatuses =
        MutableStateFlow<Map<UserWalletId, InitializationStatusFlow>>(value = emptyMap())

    override suspend fun update(userWalletId: UserWalletId, userTokens: List<LeastTokenInfo>) {
        withContext(dispatchers.io) {
            val initializationStatus = getInitializationStatusInternal(userWalletId)

            initializationStatus.update { lceLoading() }

            try {
                if (userTokens.isNotEmpty()) {
                    val response = tangemExpressApi.getAssets(
                        body = AssetsRequestBody(tokensList = userTokens),
                    ).getOrThrow()

                    expressAssetsStore.store(userWalletId, response)

                    initializationStatus.update { response.lceContent() }
                }
            } catch (e: Throwable) {
                initializationStatus.update { e.lceError() }
                Timber.e(e, "Unable to fetch assets for: ${userWalletId.stringValue}")
            }
        }
    }

    override fun getInitializationStatus(userWalletId: UserWalletId): InitializationStatusFlow {
        return getInitializationStatusInternal(userWalletId)
    }

    private fun getInitializationStatusInternal(userWalletId: UserWalletId): InitializationStatusFlow {
        val initializationStatus = initializationStatuses.value.get(key = userWalletId)

        if (initializationStatus != null) return initializationStatus

        val default: InitializationStatusFlow = MutableStateFlow(value = lceLoading())

        initializationStatuses.update {
            it.toMutableMap().apply {
                put(key = userWalletId, value = default)
            }
        }

        return default
    }
}