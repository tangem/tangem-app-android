package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.spend.datasource.pay.serialization.TangemPaySerializersModuleProvider
import kotlinx.serialization.json.jsonObject
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TangemPayTxHistoryResponseSerializationTest {

    private val json = NetworkJsonFactory.create(serializersModule = TangemPaySerializersModuleProvider.provide())

    @Test
    fun `GIVEN every known type WHEN decode THEN each item is resolved to its own transaction subtype`() {
        // Arrange
        val body = """
            {"result":{"transactions":[
              {"id":"1","type":"spend","spend":$SPEND},
              {"id":"2","type":"refund","refund":$REFUND},
              {"id":"3","type":"collateral","collateral":$COLLATERAL},
              {"id":"4","type":"payment","payment":$PAYMENT},
              {"id":"5","type":"fee","fee":$FEE}
            ]}}
        """.trimIndent()

        // Act
        val actual = json.decodeFromString(TangemPayTxHistoryResponse.serializer(), body).result.transactions

        // Assert
        assertThat(actual.map { it::class }).containsExactly(
            TangemPayTxHistoryResponse.Transaction.Spend::class,
            TangemPayTxHistoryResponse.Transaction.Refund::class,
            TangemPayTxHistoryResponse.Transaction.Collateral::class,
            TangemPayTxHistoryResponse.Transaction.Payment::class,
            TangemPayTxHistoryResponse.Transaction.Fee::class,
        ).inOrder()
        assertThat(actual.map { it.id }).containsExactly("1", "2", "3", "4", "5").inOrder()
    }

    @Test
    fun `GIVEN spend transaction WHEN decode THEN numbers and dates are parsed`() {
        // Arrange
        val body = """{"result":{"transactions":[{"id":"1","type":"spend","spend":$SPEND}]}}"""

        // Act
        val actual = json.decodeFromString(TangemPayTxHistoryResponse.serializer(), body).result.transactions.single()

        // Assert
        val spend = (actual as TangemPayTxHistoryResponse.Transaction.Spend).spend
        assertThat(spend?.amount).isEqualTo(BigDecimal("-15.5"))
        assertThat(spend?.cashback).isEqualTo(BigDecimal("0.50"))
        assertThat(spend?.authorizedAt?.toString()).isEqualTo("2026-01-02T03:04:05.000Z")
        assertThat(spend?.postedAt).isNull()
    }

    @Test
    fun `GIVEN unknown type WHEN decode THEN the item becomes Unknown and the page survives`() {
        // Arrange
        val body = """
            {"result":{"transactions":[
              {"id":"1","type":"transfer","transfer":{"amount":1}},
              {"id":"2","type":"fee","fee":$FEE}
            ]}}
        """.trimIndent()

        // Act
        val actual = json.decodeFromString(TangemPayTxHistoryResponse.serializer(), body).result.transactions

        // Assert
        assertThat(actual).containsExactly(
            TangemPayTxHistoryResponse.Transaction.Unknown(
                id = "1",
                type = "transfer",
                payload = json.parseToJsonElement("""{"id":"1","type":"transfer","transfer":{"amount":1}}""").jsonObject,
            ),
            TangemPayTxHistoryResponse.Transaction.Fee(
                id = "2",
                fee = TangemPayTxHistoryResponse.FeeData(
                    amount = BigDecimal("2"),
                    currency = "USD",
                    description = "Monthly fee",
                    postedAt = DateTime.parse("2026-01-02T03:04:05Z"),
                ),
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN item without type WHEN decode THEN it becomes Unknown with a null type`() {
        // Arrange
        val body = """{"result":{"transactions":[{"id":"1","amount":1}]}}"""

        // Act
        val actual = json.decodeFromString(TangemPayTxHistoryResponse.serializer(), body).result.transactions

        // Assert
        assertThat(actual).containsExactly(
            TangemPayTxHistoryResponse.Transaction.Unknown(
                id = "1",
                type = null,
                payload = json.parseToJsonElement("""{"id":"1","amount":1}""").jsonObject,
            ),
        )
    }

    @Test
    fun `GIVEN known type without payload WHEN decode THEN payload is null instead of failing`() {
        // Arrange
        val body = """{"result":{"transactions":[{"id":"1","type":"payment"}]}}"""

        // Act
        val actual = json.decodeFromString(TangemPayTxHistoryResponse.serializer(), body).result.transactions.single()

        // Assert
        assertThat(actual).isEqualTo(TangemPayTxHistoryResponse.Transaction.Payment(id = "1", payment = null))
    }

    @Test
    fun `GIVEN single transaction response WHEN decode THEN result is polymorphic as well`() {
        // Arrange
        val body = """{"result":{"id":"9","type":"collateral","collateral":$COLLATERAL}}"""

        // Act
        val actual = json.decodeFromString(TangemPayTransactionResponse.serializer(), body).result

        // Assert
        assertThat(actual).isInstanceOf(TangemPayTxHistoryResponse.Transaction.Collateral::class.java)
        assertThat(actual.id).isEqualTo("9")
    }

    private companion object {
        const val SPEND = """{"amount":-15.5,"currency":"USD","receipt":false,"merchant_name":"Spotify",""" +
            """"status":"COMPLETED","authorized_at":"2026-01-02T03:04:05Z","posted_at":null,""" +
            """"cashback":"0.50","cashback_status":"confirmed","cashback_currency_code":"USD"}"""
        const val REFUND = """{"amount":15.5,"currency":"USD","merchant_name":"Spotify","status":"COMPLETED",""" +
            """"authorized_at":"2026-01-02T03:04:05Z","posted_at":"2026-01-03T03:04:05Z"}"""
        const val COLLATERAL = """{"amount":100,"currency":"USDC","chain_id":137,"wallet_address":"0xabc",""" +
            """"transaction_hash":"0xhash","posted_at":"2026-01-02T03:04:05Z"}"""
        const val PAYMENT = """{"amount":50,"currency":"USDC","status":"COMPLETED",""" +
            """"posted_at":"2026-01-02T03:04:05Z"}"""
        const val FEE = """{"amount":2,"currency":"USD","description":"Monthly fee",""" +
            """"posted_at":"2026-01-02T03:04:05Z"}"""
    }
}