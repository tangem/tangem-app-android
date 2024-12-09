package com.tangem.google

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object GoogleServicesHelper {

    private val allowedCardNetworks = JSONArray(
        listOf(
            "AMEX",
            "DISCOVER",
            "INTERAC",
            "JCB",
            "MASTERCARD",
            "VISA",
        ),
    )

    private val allowedCardAuthMethods = JSONArray(
        listOf(
            "PAN_ONLY",
            "CRYPTOGRAM_3DS",
        ),
    )

    private val baseCardPaymentMethod: JSONObject = JSONObject().apply {
        val parameters = JSONObject().apply {
            put("allowedAuthMethods", allowedCardAuthMethods)
            put("allowedCardNetworks", allowedCardNetworks)
            put("billingAddressRequired", true)
            put(
                "billingAddressParameters",
                JSONObject().apply {
                    put("format", "FULL")
                },
            )
        }

        put("type", "CARD")
        put("parameters", parameters)
    }

    private val baseRequest = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
    }

    private val availabilityRequest = baseRequest.apply {
        put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod))
    }

    fun createPaymentsClient(context: Context): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .build()
        return Wallet.getPaymentsClient(context, walletOptions)
    }

    fun checkGoogleServicesAvailability(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(context)

        return status == ConnectionResult.SUCCESS
    }

    suspend fun checkGooglePayAvailability(paymentsClient: PaymentsClient): Boolean {
        val request = IsReadyToPayRequest.fromJson(availabilityRequest.toString())
        val task = paymentsClient.isReadyToPay(request)

        return suspendCoroutine<Result<Boolean>> { continuation ->
            task.addOnCompleteListener { completedTask ->
                try {
                    val result = completedTask.getResult(ApiException::class.java)
                    continuation.resume(Result.success(result))
                } catch (exception: ApiException) {
                    continuation.resume(Result.failure(exception))
                }
            }
        }.getOrElse { false }
    }
}