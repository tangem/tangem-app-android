package com.tangem.data.visa

import arrow.fx.coroutines.parZip
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.card.EllipticCurve
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.visa.utils.VisaConfig
import com.tangem.data.visa.utils.VisaCurrencyFactory
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.visa.repository.VisaRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.lib.visa.VisaContractInfoProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

internal class DefaultVisaRepository(
    private val visaContractInfoProvider: VisaContractInfoProvider,
    private val tangemTechApi: TangemTechApi,
    private val cacheRegistry: CacheRegistry,
    private val userWalletsStore: UserWalletsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : VisaRepository {

    private val currencyFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        VisaCurrencyFactory()
    }

    private val fetchedCurrencies = MutableStateFlow(
        value = hashMapOf<String, VisaCurrency>(),
    )

    override suspend fun getVisaCurrency(userWalletId: UserWalletId, isRefresh: Boolean): VisaCurrency {
        val address = makeAddress(userWalletId)
        // val address = "0x143fe062a538176aa0bf162f13d390208f90898f" // for testing

        fetchVisaCurrencyIfExpired(address, isRefresh)

        return requireNotNull(fetchedCurrencies.value[address]) {
            "Unable to find VISA currency for $address"
        }
    }

    private suspend fun fetchVisaCurrencyIfExpired(address: String, isRefresh: Boolean) {
        cacheRegistry.invokeOnExpire(
            key = getBalancesAndLimitsKey(address),
            skipCache = isRefresh,
            block = { fetchVisaCurrency(address) },
        )
    }

    private suspend fun fetchVisaCurrency(address: String) {
        parZip(
            dispatchers.io,
            { visaContractInfoProvider.getBalancesAndLimits(address) },
            { getFiatRate() },
            { balancesAndLimits, fiatRate ->
                fetchedCurrencies.update { value ->
                    value.apply {
                        put(address, currencyFactory.create(balancesAndLimits, fiatRate))
                    }
                }
            },
        )
    }

    private suspend fun getFiatRate(): BigDecimal? {
        val fiatCurrencyId = VisaConfig.fiatCurrency.code.lowercase()
        val quotes = tangemTechApi.getQuotes(
            currencyId = fiatCurrencyId,
            coinIds = VisaConfig.TOKEN_ID,
        ).getOrThrow()

        return quotes.quotes[VisaConfig.TOKEN_ID]?.price
    }

    private suspend fun makeAddress(userWalletId: UserWalletId): String {
        val userWallet = findVisaUserWallet(userWalletId)
        val walletAddresses = makeWalletAddresses(userWallet)
        val walletAddress = walletAddresses.firstOrNull { it.type == AddressType.Default }

        return requireNotNull(walletAddress?.value) {
            "Unable to find wallet address"
        }
    }

    private fun makeWalletAddresses(userWallet: UserWallet): Set<Address> {
        val walletBlockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()

        return walletBlockchain.makeAddresses(getCardPubKey(userWallet))
    }

    private fun getCardPubKey(userWallet: UserWallet): ByteArray {
        val cardWallet = userWallet.scanResponse.card.wallets.firstOrNull {
            it.curve == EllipticCurve.Secp256k1
        }
        requireNotNull(cardWallet) { "Secp256k1 card wallet not found" }

        return cardWallet.publicKey
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

    private fun getBalancesAndLimitsKey(address: String): String {
        return "visa_balances_and_limits_$address"
    }
}