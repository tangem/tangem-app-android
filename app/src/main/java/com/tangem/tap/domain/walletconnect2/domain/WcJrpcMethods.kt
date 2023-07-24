package com.tangem.tap.domain.walletconnect2.domain

import com.squareup.moshi.*
import com.tangem.datasource.di.SdkMoshi
import com.tangem.tap.domain.walletconnect2.domain.models.binance.WCBinanceTxConfirmParam
import com.tangem.tap.domain.walletconnect2.domain.models.binance.WcBinanceCancelOrder
import com.tangem.tap.domain.walletconnect2.domain.models.binance.WcBinanceTradeOrder
import com.tangem.tap.domain.walletconnect2.domain.models.binance.WcBinanceTransferOrder
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

enum class WcJrpcMethods(val code: String) {

    ETH_SIGN("eth_sign"),
    ETH_PERSONAL_SIGN("personal_sign"),
    ETH_SIGN_TYPE_DATA("eth_signTypedData"),
    ETH_SIGN_TYPE_DATA_V4("eth_signTypedData_v4"),
    ETH_SIGN_TRANSACTION("eth_signTransaction"),
    ETH_SEND_TRANSACTION("eth_sendTransaction"),
    BNB_SIGN("bnb_sign"),
    BNB_TRANSACTION_CONFIRM("bnb_tx_confirmation"),
    SIGN_TRANSACTION("trust_signTransaction"),
    WALLET_ADD_ETHEREUM_CHAIN("wallet_addEthereumChain"),
    ;

    companion object {
        fun fromCode(code: String): WcJrpcMethods? = values().firstOrNull { it.code == code }
    }
}

@JsonClass(generateAdapter = true)
data class WCSignTransaction(
    @Json(name = "network")
    val network: Int,

    @Json(name = "transaction")
    val transaction: String,
) : WcRequestData

@JsonClass(generateAdapter = true)
data class WcAddChain(
    @Json(name = "chainId")
    val chainId: String,
) : WcRequestData

@JsonClass(generateAdapter = true)
data class WcEthereumSignMessage(
    @Json(name = "raw")
    val raw: List<String>,

    @Json(name = "type")
    val type: WCSignType,
) : WcRequestData {
    enum class WCSignType {
        MESSAGE, PERSONAL_MESSAGE, TYPED_MESSAGE
    }

    /**
     * Raw parameters will always be the message and the address. Depending on the WCSignType,
     * those parameters can be swapped as description below:
     *
     *  - MESSAGE: `[address, data ]`
     *  - TYPED_MESSAGE: `[address, data]`
     *  - PERSONAL_MESSAGE: `[data, address]`
     *
     *  reference: https://docs.walletconnect.org/json-rpc/ethereum#eth_signtypeddata
     */
    val data
        get() = when (type) {
            WCSignType.PERSONAL_MESSAGE -> raw[0]
            else -> raw[1]
        }

    val address
        get() = when (type) {
            WCSignType.PERSONAL_MESSAGE -> raw[1]
            else -> raw[0]
        }
}

@JsonClass(generateAdapter = true)
data class WcEthereumTransaction(
    @Json(name = "from")
    val from: String,

    @Json(name = "to")
    val to: String?,

    @Json(name = "nonce")
    val nonce: String?,

    @Json(name = "gasPrice")
    val gasPrice: String?,

    @Json(name = "maxFeePerGas")
    val maxFeePerGas: String?,

    @Json(name = "maxPriorityFeePerGas")
    val maxPriorityFeePerGas: String?,

    @Json(name = "gas")
    val gas: String?,

    @Json(name = "gasLimit")
    val gasLimit: String?,

    @Json(name = "value")
    val value: String?,

    @Json(name = "data")
    val data: String,
) : WcRequestData

interface WcRequestData

data class WcCustomRequestData(val data: String) : WcRequestData

sealed class WcRequest(open val data: WcRequestData) {
    data class EthSign(override val data: WcEthereumSignMessage) : WcRequest(data)
    data class EthSignTransaction(override val data: WcEthereumTransaction) : WcRequest(data)
    data class EthSendTransaction(override val data: WcEthereumTransaction) : WcRequest(data)
    data class BnbTrade(override val data: WcBinanceTradeOrder) : WcRequest(data)
    data class BnbCancel(override val data: WcBinanceCancelOrder) : WcRequest(data)
    data class BnbTransfer(override val data: WcBinanceTransferOrder) : WcRequest(data)
    data class BnbTxConfirm(override val data: WCBinanceTxConfirmParam) : WcRequest(data)
    data class SignTransaction(override val data: WCSignTransaction) : WcRequest(data)
    data class AddChain(override val data: WcAddChain) : WcRequest(data)
    data class CustomRequest(override val data: WcCustomRequestData) : WcRequest(data)
}

@Singleton
class WcJrpcRequestsDeserializer @Inject constructor(@SdkMoshi private val moshi: Moshi) {

    @Suppress("ComplexMethod", "LongMethod")
    fun deserialize(method: String, params: String): WcRequest {
        val customRequest = WcRequest.CustomRequest(WcCustomRequestData(params))
        val wcMethod: WcJrpcMethods = WcJrpcMethods.fromCode(method) ?: return customRequest

        return when (wcMethod) {
            WcJrpcMethods.ETH_SIGN_TRANSACTION -> {
                val deserializedParams = moshi.adapter<List<WcEthereumTransaction>>(
                    Types.newParameterizedType(List::class.java, WcEthereumTransaction::class.java),
                ).fromJsonFirstOrNull(params) ?: return customRequest
                WcRequest.EthSignTransaction(data = deserializedParams)
            }
            WcJrpcMethods.ETH_SEND_TRANSACTION -> {
                val deserializedParams = moshi.adapter<List<WcEthereumTransaction>>(
                    Types.newParameterizedType(List::class.java, WcEthereumTransaction::class.java),
                ).fromJsonFirstOrNull(params) ?: return customRequest
                WcRequest.EthSendTransaction(data = deserializedParams)
            }
            WcJrpcMethods.ETH_SIGN -> {
                val deserializedParams = moshi.adapter<List<String>>(
                    Types.newParameterizedType(List::class.java, String::class.java),
                ).fromJsonOrNull(params) ?: return customRequest
                val data = WcEthereumSignMessage(
                    raw = deserializedParams,
                    type = WcEthereumSignMessage.WCSignType.MESSAGE,
                )
                WcRequest.EthSign(data = data)
            }
            WcJrpcMethods.ETH_PERSONAL_SIGN -> {
                val deserializedParams = moshi.adapter<List<String>>(
                    Types.newParameterizedType(List::class.java, String::class.java),
                ).fromJsonOrNull(params) ?: return customRequest
                val data = WcEthereumSignMessage(
                    raw = deserializedParams,
                    type = WcEthereumSignMessage.WCSignType.PERSONAL_MESSAGE,
                )
                WcRequest.EthSign(data = data)
            }
            WcJrpcMethods.ETH_SIGN_TYPE_DATA, WcJrpcMethods.ETH_SIGN_TYPE_DATA_V4 -> {
                val deserializedParams = listOf(
                    params.substring(params.indexOf("\"") + 1, params.indexOf("\"", startIndex = 2)),
                    params.substring(params.indexOfFirst { it == '{' }, params.indexOfLast { it == '}' } + 1),
                )
                val data = WcEthereumSignMessage(
                    deserializedParams,
                    WcEthereumSignMessage.WCSignType.TYPED_MESSAGE,
                )
                Timber.d("TypedData params: $deserializedParams")
                WcRequest.EthSign(data)
            }
            WcJrpcMethods.BNB_SIGN -> {
                return deserializeBnb(moshi, params) ?: customRequest
            }
            WcJrpcMethods.BNB_TRANSACTION_CONFIRM -> {
                val deserializedParams = moshi.adapter<List<WCBinanceTxConfirmParam>>(
                    Types.newParameterizedType(List::class.java, WCBinanceTxConfirmParam::class.java),
                ).fromJsonFirstOrNull(params) ?: return customRequest
                WcRequest.BnbTxConfirm(data = deserializedParams)
            }
            WcJrpcMethods.SIGN_TRANSACTION -> {
                val deserializedParams = moshi.adapter<List<WCSignTransaction>>(
                    Types.newParameterizedType(List::class.java, WCSignTransaction::class.java),
                ).fromJsonFirstOrNull(params) ?: return customRequest
                WcRequest.SignTransaction(data = deserializedParams)
            }
            WcJrpcMethods.WALLET_ADD_ETHEREUM_CHAIN -> {
                val deserializedParams: WcAddChain = moshi.adapter<List<WcAddChain>>(
                    Types.newParameterizedType(List::class.java, WcAddChain::class.java),
                ).fromJsonFirstOrNull(params) ?: return customRequest
                WcRequest.AddChain(data = deserializedParams)
            }
        }
    }

    private fun deserializeBnb(moshi: Moshi, params: String): WcRequest? {
        val cancelOrder = moshi.adapter<List<WcBinanceCancelOrder>>(
            Types.newParameterizedType(List::class.java, WcBinanceCancelOrder::class.java),
        ).fromJsonFirstOrNull(params)
        if (cancelOrder != null) return WcRequest.BnbCancel(cancelOrder)

        val tradeOrder = moshi.adapter<List<WcBinanceTradeOrder>>(
            Types.newParameterizedType(List::class.java, WcBinanceTradeOrder::class.java),
        ).fromJsonFirstOrNull(params)
        if (tradeOrder != null) return WcRequest.BnbTrade(tradeOrder)

        val transferOrder = moshi.adapter<List<WcBinanceTransferOrder>>(
            Types.newParameterizedType(List::class.java, WcBinanceTransferOrder::class.java),
        ).fromJsonFirstOrNull(params)
        if (transferOrder != null) return WcRequest.BnbTransfer(transferOrder)

        return null
    }

    private fun <T> JsonAdapter<T>.fromJsonOrNull(data: String): T? {
        return try {
            this.fromJson(data)
        } catch (e: Exception) {
            Timber.e(e.message)
            null
        }
    }

    private fun <T> JsonAdapter<List<T>>.fromJsonFirstOrNull(data: String): T? {
        return try {
            this.fromJson(data)?.firstOrNull()
        } catch (e: Exception) {
            Timber.e(e.message)
            null
        }
    }
}