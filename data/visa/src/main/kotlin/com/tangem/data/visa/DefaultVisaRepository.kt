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
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.visa.TangemVisaApi
import com.tangem.datasource.api.visa.models.response.VisaTxHistoryResponse
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.visa.model.VisaTxDetails
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.domain.visa.repository.VisaRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.models.requireColdWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
internal class DefaultVisaRepository @Inject constructor(
    private val visaLibLoader: VisaLibLoader,
    private val tangemTechApi: TangemTechApi,
    private val cacheRegistry: CacheRegistry,
    private val userWalletsStore: UserWalletsStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val visaApiRequestMaker: VisaApiRequestMaker,
    private val visaApi: TangemVisaApi,
    private val visaCurrencyFactory: VisaCurrencyFactory,
) : VisaRepository {

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

        fetchVisaCurrencyIfExpired(userWalletId, address, isRefresh)

        return requireNotNull(fetchedCurrencies.value[address]) {
            "Unable to find VISA currency for $address"
        }
    }

    private suspend fun fetchVisaCurrencyIfExpired(userWalletId: UserWalletId, address: String, isRefresh: Boolean) {
        cacheRegistry.invokeOnExpire(
            key = getVisaCurrencyKey(address),
            skipCache = isRefresh,
            block = { fetchVisaCurrency(userWalletId, address) },
        )
    }

    private suspend fun fetchVisaCurrency(userWalletId: UserWalletId, address: String) {
        val contractInfoProvider = visaLibLoader.getOrCreateProvider()
        val paymentAccountAddress = getPaymentAccountAddress(userWalletId)
        val userWallet = findVisaUserWallet(userWalletId)

        parZip(
            dispatchers.io,
            {
                contractInfoProvider.getContractInfo(
                    walletAddress = address,
                    paymentAccountAddress = paymentAccountAddress,
                )
            },
            { getFiatRate() },
            { contractInfo, fiatRate ->
                fetchedCurrencies.update { value ->
                    value.apply {
                        put(address, visaCurrencyFactory.create(userWallet, contractInfo, fiatRate))
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
                    requestTxHistory = { offset, pageSize -> getTxHistory(userWalletId, offset, pageSize) },
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
                walletBlockchain = userWallet.requireColdWallet().scanResponse.cardTypesResolver.getBlockchain(),
            )
        }
    }

    private suspend fun getPaymentAccountAddress(userWalletId: UserWalletId): String? = runCatching {
        val userWallet = findVisaUserWallet(userWalletId)

        val customerInfo = visaApiRequestMaker.request(userWalletId) { authHeader, _ ->
            visaApi.getCustomerInfo(
                authHeader = authHeader,
                cardId = userWallet.requireColdWallet().scanResponse.card.cardId,
            )
        }

        // There will be only one payment account (backend has filtered for us), so we can take the first one
        customerInfo.paymentAccounts.firstOrNull()?.paymentAccountAddress
    }.getOrNull()

    private suspend fun getTxHistory(userWalletId: UserWalletId, offset: Int, pageSize: Int): VisaTxHistoryResponse {
        return visaApiRequestMaker.request(
            userWalletId = userWalletId,
        ) { authHeader, accessCodeData ->
            visaApi.getTxHistory(
                authHeader = authHeader,
                customerId = accessCodeData.customerId,
                productInstanceId = accessCodeData.productInstanceId,
                limit = pageSize,
                offset = offset,
            )
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

    private suspend fun getFiatRate(): BigDecimal {
        val fiatCurrencyId = VisaConstants.fiatCurrency.code.lowercase()
        val quotes = tangemTechApi.getQuotes(
            currencyId = fiatCurrencyId,
            coinIds = VisaConstants.TOKEN_ID,
        ).getOrThrow()

        return quotes.quotes[VisaConstants.TOKEN_ID]?.price ?: error("No price found")
    }

    private fun makeWalletAddresses(userWallet: UserWallet): Set<Address> {
        val walletBlockchain = userWallet.requireColdWallet().scanResponse.cardTypesResolver.getBlockchain()

        return walletBlockchain.makeAddresses(getCardPubKey(userWallet).hexToBytes())
    }

    private fun getCardPubKey(userWallet: UserWallet): String {
        if (VisaConstants.IS_DEMO_MODE_ENABLED) return getDemoPublicKey()

        val cardWallet = userWallet.requireColdWallet().scanResponse.card.wallets.firstOrNull {
            it.curve == EllipticCurve.Secp256k1
        }
        requireNotNull(cardWallet) { "Visa card wallet not found" }

        return cardWallet.publicKey.toHexString()
    }

    private suspend fun findVisaUserWallet(userWalletId: UserWalletId): UserWallet {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "No user wallet found: $userWalletId"
        }
        if (!userWallet.requireColdWallet().scanResponse.cardTypesResolver.isVisaWallet()) {
            error("VISA wallet required: $userWalletId")
        }

        return userWallet
    }

    private fun getVisaCurrencyKey(address: String): String {
        return "visa_currency_$address"
    }
}