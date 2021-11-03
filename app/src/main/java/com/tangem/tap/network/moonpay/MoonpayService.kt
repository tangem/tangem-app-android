package com.tangem.tap.network.moonpay

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.common.services.Result
import com.tangem.common.services.retryIO
import com.tangem.tap.network.createRetrofitInstance
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MoonpayService {

    private val moonpayApi: MoonpayApi by lazy {
        createRetrofitInstance(MoonpayApi.MOOONPAY_BASE_URL)
            .create(MoonpayApi::class.java)
    }

    suspend fun getMoonpayStatus(moonpayApiKey: String): Result<MoonpayStatus> {
        return try {
            coroutineScope {

                val userStatusDeferred =
                    retryIO { async { moonpayApi.getUserStatus(moonpayApiKey) } }
                val currenciesDeferred =
                    retryIO { async { moonpayApi.getCurrencies(moonpayApiKey) } }
                val userStatus = userStatusDeferred.await()

                val currenciesToBuy = mutableListOf<String>()
                val currenciesToSell = mutableListOf<String>()

                currenciesDeferred.await().forEach { currencyStatus ->
                    if (currencyStatus.type != "crypto" || currencyStatus.isSuspended ||
                        !currencyStatus.supportsLiveMode
                    ) {
                        return@forEach
                    }

                    if (userStatus.countryCode == "USA") {
                        if (!currencyStatus.isSupportedInUS) return@forEach
                        if (currencyStatus.notAllowedUSStates.contains(userStatus.stateCode)) return@forEach
                    }
                    val currencyCode = currencyStatus.code.uppercase()
                    currenciesToBuy.add(currencyCode)

                    if (currencyStatus.isSellSupported) currenciesToSell.add(currencyCode)
                }

                Result.Success(MoonpayStatus(
                    isBuyAllowed = userStatus.isBuyAllowed,
                    isSellAllowed = userStatus.isSellAllowed,
                    availableToBuy = if (userStatus.isBuyAllowed) currenciesToBuy else emptyList(),
                    availableToSell = if (userStatus.isSellAllowed) currenciesToSell else emptyList()
                ))
            }
        } catch (error: Error) {
            Result.Failure(error)
        }
    }
}

data class MoonpayStatus(
    val isBuyAllowed: Boolean,
    val isSellAllowed: Boolean,
    val availableToBuy: List<String>,
    val availableToSell: List<String>,
)

@JsonClass(generateAdapter = true)
data class MoonPayUserStatus(
    val isBuyAllowed: Boolean,
    val isSellAllowed: Boolean,
    @Json(name = "isAllowed")
    val isMoonpayAllowed: Boolean,
    @Json(name = "alpha3")
    val countryCode: String,
    @Json(name = "state")
    val stateCode: String,
)

@JsonClass(generateAdapter = true)
data class MoonpayCurrencies(
    val type: String,
    val code: String,
    val supportsLiveMode: Boolean = false,
    val isSuspended: Boolean = true,
    val isSupportedInUS: Boolean = false,
    val isSellSupported: Boolean = false,
    val notAllowedUSStates: List<String> = emptyList(),
)