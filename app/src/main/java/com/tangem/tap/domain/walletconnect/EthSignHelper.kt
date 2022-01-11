package com.tangem.tap.domain.walletconnect

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.trustwallet.walletconnect.JSONRPC_VERSION
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage

class EthSignHelper {
    companion object {
        private val gson: Gson by lazy {
            GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
        }

        fun tryToParseEthTypedMessage(data: String): WCEthereumSignMessage? {
            val request =
                gson.fromJson<CustomJsonRpcRequests>(data)
            return if (request.method == WCMethodExtended.ETH_SIGN_TYPE_DATA_V4) {
                WCEthereumSignMessage(
                    listOf(
                        request.params[0].asString,
                        request.params[1].asString,
                    ),
                    WCEthereumSignMessage.WCSignType.TYPED_MESSAGE
                )
            } else {
                null
            }
        }

        fun tryToParseEthTypedMessageString(message: String): String? {
            return try {
                val messageString = message
                    .replace("\\", "")
                    .removePrefix("\"")
                    .removeSuffix("\"")
                val messageJson = JsonParser().parse(messageString)
                val filteredMap = gson.fromJson<Map<*,*>>(messageJson)
                    .filterKeys { it == "domain" || it == "message"}

                gson.toJson(filteredMap)
            } catch (exception: Exception) {
                null
            }

        }
    }
}

data class CustomJsonRpcRequests(
    val id: Long,
    val jsonrpc: String = JSONRPC_VERSION,
    val method: WCMethodExtended?,
    val params: JsonArray
)

enum class WCMethodExtended {
    @SerializedName("wc_sessionRequest")
    SESSION_REQUEST,

    @SerializedName("wc_sessionUpdate")
    SESSION_UPDATE,

    @SerializedName("eth_sign")
    ETH_SIGN,

    @SerializedName("personal_sign")
    ETH_PERSONAL_SIGN,

    @SerializedName("eth_signTypedData")
    ETH_SIGN_TYPE_DATA,

    @SerializedName("eth_signTypedData_v4")
    ETH_SIGN_TYPE_DATA_V4,

    @SerializedName("eth_signTransaction")
    ETH_SIGN_TRANSACTION,

    @SerializedName("eth_sendTransaction")
    ETH_SEND_TRANSACTION,

    @SerializedName("bnb_sign")
    BNB_SIGN,

    @SerializedName("bnb_tx_confirmation")
    BNB_TRANSACTION_CONFIRM,

    @SerializedName("get_accounts")
    GET_ACCOUNTS,

    @SerializedName("trust_signTransaction")
    SIGN_TRANSACTION;
}