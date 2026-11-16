package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

@Serializable
data class BankOfframpQuoteRequest(
    @SerialName("product_instance_id") val productInstanceId: String,
    @Serializable(with = BigDecimalAsNumberSerializer::class)
    @SerialName("amount")
    val amount: BigDecimal,
    @SerialName("bank_account_id") val bankAccountId: String? = null,
    @SerialName("bank_account") val bankAccount: BankOfframpPlaceOrderRequest.BankAccount? = null,
)

@OptIn(ExperimentalSerializationApi::class)
internal object BigDecimalAsNumberSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimalAsNumber", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        (encoder as JsonEncoder).encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
    }

    override fun deserialize(decoder: Decoder): BigDecimal =
        BigDecimal((decoder as JsonDecoder).decodeJsonElement().jsonPrimitive.content)
}