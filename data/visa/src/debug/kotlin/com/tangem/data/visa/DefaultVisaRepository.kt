package com.tangem.data.visa

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import arrow.fx.coroutines.parZip
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.visa.config.VisaLibLoader
import com.tangem.data.visa.utils.*
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.common.visa.TangemVisaAuthProvider
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.visa.VisaUtilities
import com.tangem.domain.visa.exception.RefreshTokenExpiredException
import com.tangem.domain.visa.model.*
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.domain.visa.repository.VisaRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.lib.visa.api.VisaApi
import com.tangem.lib.visa.model.VisaTxHistoryResponse
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.Throws

@Singleton
internal class DefaultVisaRepository @Inject constructor(
    private val visaLibLoader: VisaLibLoader,
    private val tangemTechApi: TangemTechApi,
    private val cacheRegistry: CacheRegistry,
    private val userWalletsStore: UserWalletsStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val visaAuthProvider: TangemVisaAuthProvider,
    private val visaAuthRepository: VisaAuthRepository,
) : VisaRepository {

    private val currencyFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        VisaCurrencyFactory()
    }
    private val txDetailsFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        VisaTxDetailsFactory()
    }

    private val fetchedCurrencies = MutableStateFlow(
        value = hashMapOf<String, VisaCurrency>(),
    )
    private val fetchedHistoryItems = MutableStateFlow(
        value = emptyMap<String, List<VisaTxHistoryResponse.Transaction>>(),
    )

    override suspend fun getVisaCurrency(userWalletId: UserWalletId, isRefresh: Boolean): VisaCurrency {
        val address = makeAddress(userWalletId)

        fetchVisaCurrencyIfExpired(address, isRefresh)

        return requireNotNull(fetchedCurrencies.value[address]) {
            "Unable to find VISA currency for $address"
        }
    }

    private suspend fun fetchVisaCurrencyIfExpired(address: String, isRefresh: Boolean) {
        cacheRegistry.invokeOnExpire(
            key = getVisaCurrencyKey(address),
            skipCache = isRefresh,
            block = { fetchVisaCurrency(address) },
        )
    }

    private suspend fun fetchVisaCurrency(address: String) {
        val contractInfoProvider = visaLibLoader.getOrCreateProvider()

        parZip(
            dispatchers.io,
            { contractInfoProvider.getContractInfo(address) },
            { getFiatRate() },
            { contractInfo, fiatRate ->
                fetchedCurrencies.update { value ->
                    value.apply {
                        put(address, currencyFactory.create(contractInfo, fiatRate))
                    }
                }
            },
        )
    }

    override suspend fun getTxHistory(
        userWalletId: UserWalletId,
        pageSize: Int,
        isRefresh: Boolean,
    ): Flow<PagingData<VisaTxHistoryItem>> {
        val userWallet = findVisaUserWallet(userWalletId)
        val cardPubKey = getCardPubKey(userWallet)
        val api = visaLibLoader.getOrCreateApi()
        val pager = Pager(
            config = PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
            ),
            pagingSourceFactory = {
                VisaTxHistoryPagingSource(
                    params = VisaTxHistoryPagingSource.Params(
                        cardPublicKey = cardPubKey,
                        pageSize = pageSize,
                        isRefresh = isRefresh,
                        userWallet = userWallet,
                    ),
                    cacheRegistry = cacheRegistry,
                    fetchedItems = fetchedHistoryItems,
                    dispatchers = dispatchers,
                    requestTxHistory = { offset, pageSize -> getTxHistory(api, userWalletId, offset, pageSize) },
                )
            },
        )

        return pager.flow
    }

    override suspend fun getTxDetails(userWalletId: UserWalletId, txId: String): VisaTxDetails {
        return withContext(dispatchers.io) {
            val userWallet = findVisaUserWallet(userWalletId)
            val cardPubKey = getCardPubKey(userWallet)
            val transaction = fetchedHistoryItems.value[cardPubKey]?.firstOrNull {
                it.transactionId.toString() == txId
            }
            requireNotNull(transaction) { "Transaction not found: $txId" }

            txDetailsFactory.create(
                transaction = transaction,
                walletBlockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain(),
            )
        }
    }

    private suspend fun getTxHistory(
        api: VisaApi,
        userWalletId: UserWalletId,
        offset: Int,
        pageSize: Int,
    ): VisaTxHistoryResponse = withContext(dispatchers.io) {
        val userWallet = findVisaUserWallet(userWalletId)
        val cardPubKey = getCardPubKey(userWallet)

        request(userWalletId = userWalletId) {
            api.getTxHistory(
                authorizationHeader = visaAuthProvider.getAuthHeader(userWallet.cardId),
                cardPublicKey = cardPubKey,
                limit = pageSize,
                offset = offset,
            ).getOrThrow()
        }
    }

    private suspend fun makeAddress(userWalletId: UserWalletId): String {
        if (VisaConstants.IS_DEMO_MODE_ENABLED) return getDemoAddress()

        val userWallet = findVisaUserWallet(userWalletId)
        val walletAddresses = makeWalletAddresses(userWallet)
        val walletAddress = walletAddresses.firstOrNull { it.type == AddressType.Default }

        return requireNotNull(walletAddress?.value) {
            "Unable to find wallet address"
        }
    }

    private suspend fun getFiatRate(): BigDecimal? {
        val fiatCurrencyId = VisaConstants.fiatCurrency.code.lowercase()
        val quotes = tangemTechApi.getQuotes(
            currencyId = fiatCurrencyId,
            coinIds = VisaConstants.TOKEN_ID,
        ).getOrThrow()

        return quotes.quotes[VisaConstants.TOKEN_ID]?.price
    }

    private fun makeWalletAddresses(userWallet: UserWallet): Set<Address> {
        val walletBlockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()

        return walletBlockchain.makeAddresses(getCardPubKey(userWallet).hexToBytes())
    }

    private fun getCardPubKey(userWallet: UserWallet): String {
        if (VisaConstants.IS_DEMO_MODE_ENABLED) return getDemoPublicKey()

        val cardWallet = userWallet.scanResponse.card.wallets.firstOrNull {
            it.curve == EllipticCurve.Secp256k1
        }
        requireNotNull(cardWallet) { "Visa card wallet not found" }

        return cardWallet.publicKey.toHexString()
    }

    private suspend fun findVisaUserWallet(userWalletId: UserWalletId): UserWallet {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "No user wallet found: $userWalletId"
        }
        if (!userWallet.scanResponse.cardTypesResolver.isVisaWallet()) {
            error("VISA wallet required: $userWalletId")
        }

        return userWallet
    }

    private fun getVisaCurrencyKey(address: String): String {
        return "visa_currency_$address"
    }

    private suspend fun <T : Any> request(
        userWalletId: UserWalletId,
        requestBlock: suspend () -> T,
    ): T {
        return runCatching {
            requestBlock()
        }.getOrElse { responseError ->
            if (responseError !is ApiResponseError.HttpException ||
                responseError.code != ApiResponseError.HttpException.Code.UNAUTHORIZED
            ) {
                throw responseError
            }

            val authTokens = getAuthTokens(userWalletId)
            val newTokens = runCatching {
                visaAuthRepository.refreshAccessTokens(authTokens.refreshToken)
            }.getOrElse {
                if (it is ApiResponseError.HttpException &&
                    it.code == ApiResponseError.HttpException.Code.UNAUTHORIZED
                ) {
                    userWalletsStore.update(userWalletId) { userWallet ->
                        userWallet.copy(
                            scanResponse = userWallet.scanResponse.copy(
                                visaCardActivationStatus = VisaCardActivationStatus.RefreshTokenExpired
                            )
                        )
                    }
                }
                throw RefreshTokenExpiredException()
            }

            userWalletsStore.update(userWalletId) { userWallet ->
                userWallet.copy(
                    scanResponse = userWallet.scanResponse.copy(
                        visaCardActivationStatus = VisaCardActivationStatus.Activated(
                            visaAuthTokens = newTokens
                        )
                    )
                )
            }

            requestBlock()
        }
    }

    @Throws
    private suspend fun getAuthTokens(userWalletId: UserWalletId): VisaAuthTokens {
        val userWallet = findVisaUserWallet(userWalletId)
        val status = userWallet.scanResponse.visaCardActivationStatus ?: error("Visa card activation status not found")
        return (status as? VisaCardActivationStatus.Activated)?.visaAuthTokens ?: error("Visa card is not activated")
    }
}