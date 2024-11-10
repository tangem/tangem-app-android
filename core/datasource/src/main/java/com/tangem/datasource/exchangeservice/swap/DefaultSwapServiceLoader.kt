package com.tangem.datasource.exchangeservice.swap

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.api.express.models.request.AssetsRequestBody
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
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
 * Default implementation of [SwapServiceLoader]
 *
 * @property tangemExpressApi   express api
 * @property expressAssetsStore local storage
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSwapServiceLoader @Inject constructor(
    private val tangemExpressApi: TangemExpressApi,
    private val expressAssetsStore: ExpressAssetsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : SwapServiceLoader {

    private val initializationStatuses =
        MutableStateFlow<Map<UserWalletId, InitializationStatusFlow>>(value = emptyMap())

    override suspend fun update(userWalletId: UserWalletId, userTokens: UserTokensResponse) {
        withContext(dispatchers.io) {
            val initializationStatus = getInitializationStatusInternal(userWalletId)

            initializationStatus.update { lceLoading() }

            try {
                val tokensList = userTokens.tokens.map {
                    LeastTokenInfo(
                        contractAddress = it.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE,
                        network = it.networkId,
                    )
                }

                if (tokensList.isNotEmpty()) {
                    val response = tangemExpressApi.getAssets(
                        body = AssetsRequestBody(tokensList = tokensList),
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