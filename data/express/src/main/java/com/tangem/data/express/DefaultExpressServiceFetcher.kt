package com.tangem.data.express

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.data.express.converter.ExpressAssetConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.request.AssetsRequestBody
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.exchangeservice.swap.ExpressUtils.getRefCode
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.ExpressAssetsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

typealias InitializationStatusFlow = MutableStateFlow<Lce<Throwable, List<ExpressAsset>>>

/**
 * Default implementation of [ExpressServiceFetcher]
 *
 * @property tangemExpressApi   express api
 * @property expressAssetsStore local storage
 *
[REDACTED_AUTHOR]
 */
internal class DefaultExpressServiceFetcher @Inject constructor(
    private val tangemExpressApi: TangemExpressApi,
    private val expressAssetsStore: ExpressAssetsStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val userWalletsStore: UserWalletsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : ExpressServiceFetcher {

    private val initializationStatuses =
        MutableStateFlow<Map<UserWalletId, InitializationStatusFlow>>(value = emptyMap())

    override suspend fun fetch(userWalletId: UserWalletId, assetIds: Set<ExpressAsset.ID>): Either<Throwable, Unit> =
        either {
            val userWallet = arrow.core.raise.catch(
                block = { userWalletsStore.getSyncStrict(userWalletId) },
                catch = ::raise,
            )

            fetch(userWallet = userWallet, assetIds = assetIds).bind()
        }

    override suspend fun fetch(userWallet: UserWallet, assetIds: Set<ExpressAsset.ID>): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.io) {
            val initializationStatus = getInitializationStatusInternal(userWallet.walletId)

            try {
                if (assetIds.isNotEmpty()) {
                    val tokenList = assetIds.map {
                        LeastTokenInfo(contractAddress = it.contractAddress, network = it.networkId)
                    }

                    val response = tangemExpressApi.getAssets(
                        userWalletId = userWallet.walletId.stringValue,
                        refCode = getRefCode(userWallet, appPreferencesStore),
                        body = AssetsRequestBody(tokensList = tokenList),
                    ).getOrThrow()

                    expressAssetsStore.store(userWallet.walletId, response)

                    val expressAssets = ExpressAssetConverter.convertList(response)
                    initializationStatus.update { expressAssets.lceContent() }
                }
            } catch (e: Throwable) {
                if (expressAssetsStore.getSyncOrNull(userWallet.walletId) == null) {
                    initializationStatus.update { e.lceError() }
                }
                Timber.e(e, "Unable to fetch assets for: ${userWallet.walletId.stringValue}")
                throw e
            }
        }
    }

    override fun getInitializationStatus(userWalletId: UserWalletId): Flow<Lce<Throwable, List<ExpressAsset>>> {
        return flow { getInitializationStatusInternal(userWalletId).collect { emit(it) } }
    }

    @Suppress("SuspendFunWithFlowReturnType")
    private suspend fun getInitializationStatusInternal(userWalletId: UserWalletId): InitializationStatusFlow {
        val initializationStatus = initializationStatuses.value[userWalletId]
        if (initializationStatus != null) return initializationStatus

        val cached = expressAssetsStore.getSyncOrNull(userWalletId)?.let(ExpressAssetConverter::convertList)
        val default: InitializationStatusFlow = MutableStateFlow(value = cached?.lceContent() ?: lceLoading())

        initializationStatuses.update { statuses ->
            statuses.toMutableMap().apply {
                this[userWalletId] = default
            }
        }

        return default
    }
}