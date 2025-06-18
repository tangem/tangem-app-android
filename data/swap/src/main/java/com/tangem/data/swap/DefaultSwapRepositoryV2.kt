package com.tangem.data.swap

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.swap.converter.TokenInfoConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.request.PairsRequestBody
import com.tangem.datasource.exchangeservice.swap.ExpressUtils
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.express.ExpressRepository
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.models.SwapPairModel
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
internal class DefaultSwapRepositoryV2 @Inject constructor(
    private val tangemExpressApi: TangemExpressApi,
    private val expressRepository: ExpressRepository,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val appPreferencesStore: AppPreferencesStore,
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
) : SwapRepositoryV2 {

    private val tokenInfoConverter = TokenInfoConverter()
    override suspend fun getPairs(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyStatusList: List<CryptoCurrencyStatus>,
    ): List<SwapPairModel> = withContext(coroutineDispatcher.io) {
        val cryptoCurrencyList = cryptoCurrencyStatusList.map { it.currency }

        val allPairs = getPairsInternal(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            cryptoCurrencyList = cryptoCurrencyList,
        )

        val providers = expressRepository.getProviders(userWallet = userWallet)
        val mappedProviders = providers.associateBy(ExpressProvider::providerId)

        allPairs.map { pair ->
            async {
                val statusFrom = cryptoCurrencyStatusList
                    .firstOrNull {
                        it.currency.getContractAddress() == pair.from.contractAddress &&
                            it.currency.network.backendId == pair.from.network
                    }
                val statusTo = cryptoCurrencyStatusList
                    .firstOrNull {
                        it.currency.getContractAddress() == pair.to.contractAddress &&
                            it.currency.network.backendId == pair.to.network
                    }

                if (statusFrom != null && statusTo != null) {
                    SwapPairModel(
                        from = statusFrom,
                        to = statusTo,
                        providers = pair.providers.mapNotNull {
                            mappedProviders[it.providerId]
                        },
                    )
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun getPairsOnly(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyList: List<CryptoCurrency>,
    ): List<SwapPairModel> = withContext(coroutineDispatcher.io) {
        val allPairs = getPairsInternal(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            cryptoCurrencyList = cryptoCurrencyList,
        )

        allPairs.map { pair ->
            async {
                val statusFromDeferred = async {
                    cryptoCurrencyList
                        .firstOrNull {
                            it.getContractAddress() == pair.from.contractAddress &&
                                it.network.backendId == pair.from.network
                        }
                }
                val statusToDeferred = async {
                    cryptoCurrencyList
                        .firstOrNull {
                            it.getContractAddress() == pair.to.contractAddress &&
                                it.network.backendId == pair.to.network
                        }
                }

                createPairModelOnly(
                    currencyFrom = statusFromDeferred.await(),
                    currencyTo = statusToDeferred.await(),
                    userWalletId = userWallet.walletId,
                )
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun CoroutineScope.getPairsInternal(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyList: List<CryptoCurrency>,
    ) = awaitAll(
        // original pairs
        async {
            invokePairRequest(
                userWallet = userWallet,
                from = arrayListOf(initialCurrency),
                to = cryptoCurrencyList,
            )
        },
        // reversed pairs
        async {
            invokePairRequest(
                userWallet = userWallet,
                from = cryptoCurrencyList,
                to = arrayListOf(initialCurrency),
            )
        },
    ).flatten()

    private suspend fun invokePairRequest(
        userWallet: UserWallet,
        from: List<CryptoCurrency>,
        to: List<CryptoCurrency>,
    ) = safeApiCall(
        call = {
            tangemExpressApi.getPairs(
                userWalletId = userWallet.walletId.stringValue,
                refCode = ExpressUtils.getRefCode(
                    userWallet = userWallet,
                    appPreferencesStore = appPreferencesStore,
                ),
                body = PairsRequestBody(
                    from = tokenInfoConverter.convertList(from),
                    to = tokenInfoConverter.convertList(to),
                ),
            ).getOrThrow()
        },
        onError = {
            Timber.w(it, "Unable to get pairs")
            throw it
        },
    )

    private suspend fun CoroutineScope.createPairModelOnly(
        currencyFrom: CryptoCurrency?,
        currencyTo: CryptoCurrency?,
        userWalletId: UserWalletId,
    ): SwapPairModel? {
        return if (currencyFrom != null && currencyTo != null) {
            val statusFrom = currencyStatusOperations.getCurrencyStatusSync(
                userWalletId = userWalletId,
                cryptoCurrencyId = currencyFrom.id,
            ).getOrNull()
            val statusTo = currencyStatusOperations.getCurrencyStatusSync(
                userWalletId = userWalletId,
                cryptoCurrencyId = currencyTo.id,
            ).getOrNull()

            if (statusFrom != null && statusTo != null) {
                SwapPairModel(
                    from = statusFrom,
                    to = statusTo,
                    providers = emptyList(),
                )
            } else {
                null
            }
        } else {
            null
        }
    }

    private fun CryptoCurrency.getContractAddress(): String {
        return when (this) {
            is CryptoCurrency.Token -> this.contractAddress
            is CryptoCurrency.Coin -> "0"
        }
    }
}