package com.tangem.tap.domain

import android.net.Uri
import android.util.Base64
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TopUpHelper {

    companion object {
//        const val REDIRECT_URL = "https://success.tangem.com"

        private const val BASE_URL = "https://buy.moonpay.io"
        private const val API_KEY_PATH = "?apiKey="
        private const val CURRENCY_PATH = "&currencyCode="
        private const val WALLET_ADDRESS_PATH = "&walletAddress="
//        private const val REDIRECT_URL_PATH = "&redirectUrl="
        private const val SIGNATURE_PATH = "&signature="

        fun getUrl(cryptoCurrencyName: CryptoCurrencyName, walletAddress: String, apiKey: String, secretKey: String): String {
            val originalQuery = API_KEY_PATH + apiKey.urlEncode() +
                    CURRENCY_PATH + cryptoCurrencyName.urlEncode() +
                    WALLET_ADDRESS_PATH + walletAddress.urlEncode()
//                    REDIRECT_URL_PATH + REDIRECT_URL.urlEncode()
            val signature = createSignature(originalQuery, secretKey)

            return BASE_URL + originalQuery + SIGNATURE_PATH + signature.urlEncode()
        }

        private fun String.urlEncode(): String {
            return Uri.encode(this)
        }


        private fun createSignature(data: String, key: String): String {
            val sha256Hmac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
            sha256Hmac.init(secretKey)
            val sha256encoded = sha256Hmac.doFinal(data.toByteArray())
            return Base64.encodeToString(sha256encoded, Base64.NO_WRAP)
        }
    }
}