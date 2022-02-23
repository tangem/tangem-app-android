package com.tangem.tap.common.shop

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.shop.googlepay.GooglePayUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GooglePayService(private val paymentsClient: PaymentsClient, private val activity: Activity) {

//    var responseCallback: ((Result<PaymentData>) -> Unit)? = null

    suspend fun checkIfGooglePayAvailable(): Result<Boolean> {

        val isReadyToPayJson = GooglePayUtil.isReadyToPayRequest() ?: return Result.success(false)
        val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString())

        val task = paymentsClient.isReadyToPay(request)
        return withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                task.addOnCompleteListener { completedTask ->
                    try {
                        val result = completedTask.getResult(ApiException::class.java)
                        continuation.resume(Result.success(true))
                    } catch (exception: ApiException) {
                        // Process error
                        Timber.w("isReadyToPay failed: $exception")
                        continuation.resume(Result.failure(exception))
                    }
                }
            }
        }
    }

    fun payWithGooglePay(totalPriceCents: String, currencyCode: String, merchantID: String) {
        val paymentDataRequestJson = GooglePayUtil.getPaymentDataRequest(
            totalPriceCents,
            currencyCode = currencyCode,
            countryCode = "RU",
            merchantID = merchantID
        )
        if (paymentDataRequestJson == null) {
            Timber.e("RequestPayment: can't fetch payment data request")
            return
        }
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request), activity, LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    fun handleResponseFromGooglePay(resultCode: Int, data: Intent?): Result<PaymentData> {
        val result = when (resultCode) {
            RESULT_OK -> {
                val paymentData = data?.let { intent -> PaymentData.getFromIntent(intent) }
                if (paymentData == null) {
                    Result.failure(Exception("No payment data"))
                } else {
                    Result.success(paymentData)
                }
            }
            RESULT_CANCELED -> {
                Result.failure(TangemSdkError.UserCancelled())
            }
            AutoResolveHelper.RESULT_ERROR -> {
                val statusCode = AutoResolveHelper.getStatusFromIntent(data)?.statusCode
                if (statusCode == null) {
                    Result.failure(Exception("Unknown Status"))
                } else {
                    Result.failure(Exception("$statusCode"))
                }

            }
            else -> Result.failure(Exception("Unknown Status"))
        }
//        responseCallback?.invoke(result)
        return result
    }

    fun parsePaymentData(paymentData: PaymentData): GooglePayResponse? {
        val paymentInformation = paymentData.toJson()

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData =
                JSONObject(paymentInformation).getJSONObject("paymentMethodData")
            val addressJson = paymentMethodData.getJSONObject("info")
                .getJSONObject("billingAddress")

            val address = Address(
                name = addressJson.getString("name"),
                postalCode = addressJson.getString("postalCode"),
                countryCode = addressJson.getString("countryCode"),
                phoneNumber = addressJson.getString("phoneNumber"),
                address1 = addressJson.getString("address1"),
                address2 = addressJson.getString("address2"),
                address3 = addressJson.getString("address3"),
                locality = addressJson.getString("locality"),
                administrativeArea = addressJson.getString("administrativeArea"),
                sortingCode = addressJson.getString("sortingCode"),
            )

            val token = paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token")

            return GooglePayResponse(address, token)

        } catch (e: JSONException) {
            Log.e("handlePaymentSuccess", "Error: " + e.toString())
        }
        return null
    }

    companion object {
        const val LOAD_PAYMENT_DATA_REQUEST_CODE = 315
    }
}

data class GooglePayResponse(
    val billingAddress: Address,
    val token: String,
)

data class Address(
    val name: String,
    val postalCode: String,
    val countryCode: String,
    val phoneNumber: String,
    val address1: String,
    val address2: String,
    val address3: String,
    val locality: String,
    val administrativeArea: String,
    val sortingCode: String
)