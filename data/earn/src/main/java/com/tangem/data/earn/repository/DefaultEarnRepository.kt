package com.tangem.data.earn.repository

import arrow.core.Either
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.earn.converter.EarnTokenConverter
import com.tangem.data.earn.datastore.EarnNetworksStore
import com.tangem.data.earn.datastore.EarnTopTokensStore
import com.tangem.data.earn.repository.batch.EarnTokensBatchFetcher
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.EarnResponse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.EarnErrorResolver
import com.tangem.domain.earn.model.EarnTokensBatchFlow
import com.tangem.domain.earn.model.EarnTokensBatchingContext
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import kotlinx.coroutines.flow.Flow

@Suppress("LongParameterList")
internal class DefaultEarnRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val earnNetworksStore: EarnNetworksStore,
    private val earnTopTokensStore: EarnTopTokensStore,
    private val earnErrorResolver: EarnErrorResolver,
    private val excludedBlockchains: ExcludedBlockchains,
) : EarnRepository {

    private val cryptoCurrencyFactory: CryptoCurrencyFactory by lazy {
        CryptoCurrencyFactory(excludedBlockchains = excludedBlockchains)
    }

    override fun getEarnTokensBatchFlow(context: EarnTokensBatchingContext, batchSize: Int): EarnTokensBatchFlow {
        val batchFetcher = EarnTokensBatchFetcher(
            tangemTechApi = tangemTechApi,
            batchSize = batchSize,
            userWalletsListRepository = userWalletsListRepository,
            cryptoCurrencyFactory = cryptoCurrencyFactory,
        )

        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: INITIAL_BATCH_KEY },
            batchFetcher = batchFetcher,
        ).toBatchFlow()
    }

    override suspend fun fetchEarnNetworks(type: String) {
        runCatching(dispatchers.io) {
            val response = tangemTechApi.getEarnNetworks(type = type).getOrThrow()
            val items = response.items.map { dto ->
                EarnNetwork(
                    networkId = dto.networkId,
                    isAdded = false,
                )
            }
            earnNetworksStore.store(Either.Right(items))
        }.onFailure { error ->
            val earnError = earnErrorResolver.resolve(error)
            earnNetworksStore.store(Either.Left(earnError))
        }
    }

    override fun observeEarnNetworks(): Flow<EarnNetworks?> {
        return earnNetworksStore.get()
    }

    override suspend fun fetchTopEarnTokens(limit: Int) {
        runCatching(dispatchers.io) {
            val response = tangemTechApi.getEarnTokens(
                isForEarn = true,
                limit = limit,
            ).getOrThrow()

            val userWallet = userWalletsListRepository
                .selectedUserWallet
                .value
                .takeIf { wallet -> wallet?.isLocked?.not() == true }
                ?: return@runCatching

            val items = response.items.mapNotNull { dto ->
                val earnToken = EarnTokenConverter.convert(dto)
                createCryptoCurrencyForEarnToken(
                    cryptoCurrencyFactory = cryptoCurrencyFactory,
                    userWallet = userWallet,
                    earnToken = dto,
                )?.let { cryptoCurrency ->
                    EarnTokenWithCurrency(
                        earnToken = earnToken,
                        cryptoCurrency = cryptoCurrency,
                        networkName = Blockchain.fromNetworkId(dto.networkId)?.fullName.orEmpty(),
                    )
                }
            }
            earnTopTokensStore.store(Either.Right(items))
        }.onFailure { error ->
            val earnError = earnErrorResolver.resolve(error)
            earnTopTokensStore.store(Either.Left(earnError))
        }
    }

    override fun observeTopEarnTokens(): Flow<EarnTopToken?> {
        return earnTopTokensStore.get()
    }

    companion object {
        internal const val FIRST_PAGE = 1
        private const val INITIAL_BATCH_KEY = 0
    }
}

/*
use this CryptoCurrency only for creating cryptoCurrencyIcon!! because accountIndex = DerivationIndex.Main and it
doesn't provide real all data.
 */
internal fun createCryptoCurrencyForEarnToken(
    userWallet: UserWallet,
    earnToken: EarnResponse,
    cryptoCurrencyFactory: CryptoCurrencyFactory,
): CryptoCurrency? {
    val blockchain = Blockchain.fromNetworkId(earnToken.networkId) ?: Blockchain.Unknown
    return if (earnToken.token.address.isEmpty()) {
        cryptoCurrencyFactory.createCoin(
            blockchain = blockchain,
            extraDerivationPath = null,
            userWallet = userWallet,
            accountIndex = DerivationIndex.Main,
        )
    } else {
        val network = cryptoCurrencyFactory.networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = null,
            userWallet = userWallet,
            accountIndex = DerivationIndex.Main,
        ) ?: return null

        cryptoCurrencyFactory.createToken(
            network = network,
            rawId = CryptoCurrency.RawID(earnToken.token.id),
            name = earnToken.token.name,
            symbol = earnToken.token.symbol,
            decimals = earnToken.token.decimalCount ?: blockchain.decimals(),
            contractAddress = earnToken.token.address,
        )
    }
}