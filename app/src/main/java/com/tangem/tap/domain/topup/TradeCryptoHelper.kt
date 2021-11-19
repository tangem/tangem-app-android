package com.tangem.tap.domain.topup

import android.net.Uri
import android.util.Base64
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TradeCryptoHelper {

    companion object {

        const val TRANSACTION_ID_PARAM = "transactionId"
        const val CURRENCY_CODE_PARAM = "baseCurrencyCode"
        const val CURRENCY_AMOUNT_PARAM = "baseCurrencyAmount"
        const val DEPOSIT_WALLET_ADDRESS_PARAM = "depositWalletAddress"

        private const val REDIRECT_URL_BUY = "tangem://success.tangem.com"
        private const val REDIRECT_URL_SELL_REQUEST = "tangem://sell-request.tangem.com"

        private const val BASE_URL_BUY =
            "https://buy.moonpay.io"
//        "https://buy-staging.moonpay.io" // TESTNET
        private const val BASE_URL_SELL =
            "https://sell.moonpay.com"
//        "https://sell-staging.moonpay.com" // TESTNET

        private const val API_KEY_PATH = "?apiKey="
        private const val CURRENCY_PATH = "&currencyCode="
        private const val WALLET_ADDRESS_PATH = "&walletAddress="

        private const val REFUND_WALLET_ADDRESS_PATH = "&refundWalletAddress="
        private const val BASE_CURRENCY_PATH = "&baseCurrencyCode="

        private const val REDIRECT_URL_PATH = "&redirectURL="
        private const val SIGNATURE_PATH = "&signature="

        private const val TRANSACTION_RECEIPT_PATH = "transaction_receipt?transactionId="

        fun getUrl(
            action: Action,
            blockchain: Blockchain?,
            cryptoCurrencyName: CryptoCurrencyName,
            walletAddress: String,
            apiKey: String,
            secretKey: String,
        ): String {
            val originalQuery: String
            val baseUrl: String
            when (action) {
                Action.Buy -> {
                    if (blockchain?.isTestnet() == true) {
                        return blockchain.getTestnetTopUpUrl() ?: ""
                    }
                    baseUrl = BASE_URL_BUY
                    originalQuery = API_KEY_PATH + apiKey.urlEncode() +
                            CURRENCY_PATH + cryptoCurrencyName.urlEncode() +
                            WALLET_ADDRESS_PATH + walletAddress.urlEncode() +
                            REDIRECT_URL_PATH + REDIRECT_URL_BUY.urlEncode()
                }
                Action.Sell -> {
                    baseUrl = BASE_URL_SELL
                    originalQuery = API_KEY_PATH + apiKey.urlEncode() +
                            BASE_CURRENCY_PATH + cryptoCurrencyName.urlEncode() +
                            REFUND_WALLET_ADDRESS_PATH + walletAddress.urlEncode() +
                            REDIRECT_URL_PATH + REDIRECT_URL_SELL_REQUEST.urlEncode()
                }
            }
            val signature = createSignature(originalQuery, secretKey)
            return baseUrl + originalQuery + SIGNATURE_PATH + signature.urlEncode()

        }

        fun getSellCryptoReceiptUrl(transactionId: String): String {
            return BASE_URL_SELL + TRANSACTION_RECEIPT_PATH + transactionId
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

    enum class Action { Buy, Sell }
}
