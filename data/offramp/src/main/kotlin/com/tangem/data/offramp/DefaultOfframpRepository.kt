package com.tangem.data.offramp

import android.net.Uri
import android.util.Base64
import androidx.datastore.core.DataStore
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.data.offramp.converter.PendingOfframpEntryConverter
import com.tangem.data.offramp.model.PendingOfframpEntry
import com.tangem.data.offramp.models.MoonPayAvailableCurrency
import com.tangem.domain.card.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.extensions.withIOContext
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.model.PendingOfframp
import com.tangem.domain.offramp.model.PendingOfframp.Companion.EXPIRY_MS
import com.tangem.domain.offramp.repository.OfframpInitializationStatus
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.grow.datasource.moonpay.MoonPayApi
import com.tangem.grow.datasource.moonpay.MoonPayCurrencies
import com.tangem.grow.datasource.moonpay.MoonPayUserStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class DefaultOfframpRepository(
    private val api: MoonPayApi,
    private val credentials: MoonPayCredentials,
    private val userWalletProvider: () -> UserWallet?,
    private val appThemeModeHolder: AppThemeModeHolder,
    private val pendingOfframpStore: DataStore<List<PendingOfframpEntry>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : OfframpRepository {

    private val pendingOfframpConverter = PendingOfframpEntryConverter()

    override val initializationStatus: StateFlow<OfframpInitializationStatus>
        field = MutableStateFlow<OfframpInitializationStatus>(value = lceLoading())

    private var status: MoonPayStatus? = null

    override suspend fun update() {
        withIOContext {
            TangemLogger.i("Start updating")
            initializationStatus.value = lceLoading()

            performRequest {
                val userStatus = when (val result = performRequest { api.getUserStatus(credentials.apiKey) }) {
                    is Result.Failure -> {
                        TangemLogger.e("Failed to load user status", result.error)
                        initializationStatus.value = result.error.lceError()
                        return@performRequest
                    }
                    is Result.Success -> result.data
                }

                val currencies = when (val result = performRequest { api.getCurrencies(credentials.apiKey) }) {
                    is Result.Failure -> {
                        TangemLogger.e("Failed to load currencies", result.error)
                        initializationStatus.value = result.error.lceError()
                        return@performRequest
                    }
                    is Result.Success -> result.data
                }

                val currenciesToSell = currencies
                    .filter { currency ->
                        checkGeneralRequirements(currency) && checkUSARequirements(userStatus, currency)
                    }
                    .mapNotNull { currency ->
                        if (!currency.isSellSupported) return@mapNotNull null
                        MoonPayAvailableCurrency(
                            currencyCode = currency.code,
                            networkCode = currency.metadata?.networkCode ?: return@mapNotNull null,
                            contractAddress = currency.metadata?.contractAddress,
                        )
                    }

                TangemLogger.i("Successfully updated")
                initializationStatus.value = lceContent()
                status = MoonPayStatus(currenciesToSell, userStatus, currencies)
            }
        }
    }

    private fun checkGeneralRequirements(currency: MoonPayCurrencies): Boolean {
        return currency.type == "crypto" && !currency.isSuspended && currency.supportsLiveMode &&
            currency.isSellSupported
    }

    private fun checkUSARequirements(userStatus: MoonPayUserStatus, currency: MoonPayCurrencies): Boolean {
        return if (userStatus.countryCode == "USA") {
            currency.isSupportedInUS && !currency.notAllowedUSStates.contains(userStatus.stateCode)
        } else {
            true
        }
    }

    override fun availableForSell(cryptoCurrency: CryptoCurrency): Boolean {
        val userWallet = userWalletProvider() ?: return false
        val isExchangeSupported = userWallet !is UserWallet.Cold || !userWallet.scanResponse.card.isStart2Coin

        if (!isExchangeSupported) return false
        if (!isSellAllowed()) return false

        val availableForSell = status?.availableForSell ?: return false
        val blockchain = cryptoCurrency.network.toBlockchain()
        val supportedCurrency = blockchain.moonPaySupportedCurrency ?: return false
        return availableForSell.any { availableCurrency ->
            when (cryptoCurrency) {
                is CryptoCurrency.Coin -> {
                    availableCurrency.networkCode.equals(other = supportedCurrency.networkCode, ignoreCase = true) &&
                        availableCurrency.currencyCode.equals(other = supportedCurrency.currencyCode, ignoreCase = true)
                }
                is CryptoCurrency.Token -> {
                    availableCurrency.networkCode.equals(other = supportedCurrency.networkCode, ignoreCase = true) &&
                        availableCurrency.contractAddress.equals(
                            other = cryptoCurrency.contractAddress,
                            ignoreCase = true,
                        )
                }
            }
        }
    }

    override fun getOfframpUrl(
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyCode: String,
        walletAddress: String,
        requestId: String,
    ): String? {
        val blockchain = cryptoCurrency.network.toBlockchain()
        if (blockchain.isTestnet()) return blockchain.getTestnetTopUpUrl()

        val supportedCurrency = blockchain.moonPaySupportedCurrency ?: return null
        val moonpayCurrency = status?.availableForSell?.firstOrNull { availableCurrency ->
            when (cryptoCurrency) {
                is CryptoCurrency.Coin -> {
                    availableCurrency.networkCode.equals(other = supportedCurrency.networkCode, ignoreCase = true) &&
                        availableCurrency.currencyCode.equals(other = supportedCurrency.currencyCode, ignoreCase = true)
                }
                is CryptoCurrency.Token -> {
                    availableCurrency.networkCode.equals(other = supportedCurrency.networkCode, ignoreCase = true) &&
                        availableCurrency.contractAddress.equals(
                            other = cryptoCurrency.contractAddress,
                            ignoreCase = true,
                        )
                }
            }
        } ?: return null

        val uri = Uri.Builder()
            .scheme(SCHEME)
            .authority(URL_SELL)
            .appendQueryParameter("apiKey", credentials.apiKey)
            .appendQueryParameter("baseCurrencyCode", moonpayCurrency.currencyCode.uppercase())
            .appendQueryParameter("refundWalletAddress", walletAddress)
            // request_id authenticates the returning redirect_sell deeplink. It must be added to
            // redirectURL BEFORE createSignature below so it is covered by the MoonPay URL signature.
            .appendQueryParameter(
                "redirectURL",
                "tangem://redirect_sell?currency_id=${cryptoCurrency.id.value}&request_id=$requestId",
            )

        if (appThemeModeHolder.isDarkThemeActive) uri.appendQueryParameter("theme", "dark")

        val originalQuery = uri.build().encodedQuery ?: uri.build().toString()
        val signature = createSignature(originalQuery)
        uri.appendQueryParameter("signature", signature)

        return uri.build().toString()
    }

    private fun createSignature(data: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(credentials.secretKey.toByteArray(), "HmacSHA256")
        sha256Hmac.init(secretKeySpec)
        val sha256encoded = sha256Hmac.doFinal("?$data".toByteArray())
        return Base64.encodeToString(sha256encoded, Base64.NO_WRAP)
    }

    private fun isSellAllowed(): Boolean {
        return status?.responseUserStatus?.isSellAllowed == true
    }

    override suspend fun registerPendingOfframp(userWalletId: UserWalletId, currencyId: String): String =
        withContext(dispatchers.io) {
            val requestId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            pendingOfframpStore.updateData { stored ->
                stored.filterNotExpired(now) + PendingOfframpEntry(
                    requestId = requestId,
                    userWalletId = userWalletId.stringValue,
                    currencyId = currencyId,
                    createdAt = now,
                )
            }
            requestId
        }

    override suspend fun resolvePendingOfframp(
        requestId: String,
        userWalletId: UserWalletId,
        currencyId: String,
    ): PendingOfframp? = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        var matched: PendingOfframpEntry? = null
        pendingOfframpStore.updateData { stored ->
            matched = stored.firstOrNull { entry ->
                entry.requestId == requestId &&
                    entry.userWalletId == userWalletId.stringValue &&
                    entry.currencyId == currencyId &&
                    !entry.isExpired(now)
            }
            // Keep the matched record so the same redirect can be followed again until it expires; only prune the
            // expired ones. The record is dropped naturally once it ages past EXPIRY_MS.
            stored.filterNotExpired(now)
        }
        matched?.let(pendingOfframpConverter::convert)
    }

    override suspend fun getAllStoredOfframps(): List<PendingOfframp> = withContext(dispatchers.io) {
        pendingOfframpStore.data.first().map(pendingOfframpConverter::convert)
    }

    // Returns the same instance when nothing is expired, so DataStore.updateData sees an unchanged value and skips
    // both the extra allocation and the write.
    private fun List<PendingOfframpEntry>.filterNotExpired(now: Long): List<PendingOfframpEntry> =
        if (none { now - it.createdAt >= EXPIRY_MS }) this else filter { now - it.createdAt < EXPIRY_MS }

    private fun PendingOfframpEntry.isExpired(now: Long): Boolean = pendingOfframpConverter.convert(this).isExpired(now)

    private companion object {
        const val URL_SELL = "sell.moonpay.com"
        const val SCHEME = "https"
    }
}

private data class MoonPayStatus(
    val availableForSell: List<MoonPayAvailableCurrency>,
    val responseUserStatus: MoonPayUserStatus,
    val responseCurrencies: List<MoonPayCurrencies>,
)

internal data class MoonPayCredentials(val apiKey: String, val secretKey: String)