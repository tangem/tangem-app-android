package com.tangem.data.visa.utils

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.spend.datasource.pay.models.response.TangemPayTxHistoryResponse
import com.tangem.test.core.ProvideTestModels
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Currency

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayTxHistoryItemConverterTest {

    private val moshi = Moshi.Builder()
        .add(BigDecimal::class.java, BigDecimalTestAdapter())
        .add(DateTime::class.java, DateTimeTestAdapter())
        .build()

    private val refundAdapter = moshi.adapter(TangemPayTxHistoryResponse.Refund::class.java)
    private val collateralAdapter = moshi.adapter(TangemPayTxHistoryResponse.Collateral::class.java)
    private val transactionAdapter = moshi.adapter(TangemPayTxHistoryResponse.Transaction::class.java)

    private val converter = TangemPayTxHistoryItemConverter(moshi)

    @Test
    fun `GIVEN refund transaction WHEN convert THEN spend item with negative amounts`() {
        // Arrange
        val refund = refund()
        val transaction = TangemPayTxHistoryResponse.Transaction(id = TX_ID, type = "refund", refund = refund)
        val expected = TangemPayTxHistoryItem.Spend(
            id = TX_ID,
            jsonRepresentation = refundAdapter.toJson(refund),
            date = POSTED_AT.withZone(DateTimeZone.getDefault()),
            amount = BigDecimal("-15.00"),
            currency = Currency.getInstance("USD"),
            authorizedAmount = BigDecimal.ZERO,
            localAmount = BigDecimal("-13.50"),
            localCurrency = Currency.getInstance("EUR"),
            enrichedMerchantName = "Spotify",
            merchantName = "Spotify Premium Renewal",
            enrichedMerchantCategory = "Music",
            merchantCategoryCode = "5812",
            merchantCategory = "Entertainment",
            status = TangemPayTxHistoryItem.Status.COMPLETED,
            enrichedMerchantIconUrl = "https://example.com/icon.png",
            declinedReason = null,
            cardName = "Plus Card 1",
            cardNumberLast4 = "6460",
            cashback = TangemPayTxHistoryItem.Cashback(
                status = TangemPayTxHistoryItem.Cashback.Status.CONFIRMED,
                amount = BigDecimal("-7.50"),
                currency = Currency.getInstance("USD"),
                isCapTrimmed = false,
                exclusionReason = null,
            ),
        )

        // Act
        val actual = converter.convert(transaction)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN refund with positive amounts and no posted date WHEN convert THEN sign forced and authorized date used`() {
        // Arrange
        val refund = refund(
            amount = BigDecimal("15.00"),
            localAmount = BigDecimal("13.50"),
            cashback = BigDecimal("7.50"),
            postedAt = null,
        )
        val transaction = TangemPayTxHistoryResponse.Transaction(id = TX_ID, type = "refund", refund = refund)

        // Act
        val actual = converter.convert(transaction) as TangemPayTxHistoryItem.Spend

        // Assert
        assertThat(actual.amount).isEqualTo(BigDecimal("-15.00"))
        assertThat(actual.localAmount).isEqualTo(BigDecimal("-13.50"))
        assertThat(actual.cashback?.amount).isEqualTo(BigDecimal("-7.50"))
        assertThat(actual.date).isEqualTo(AUTHORIZED_AT.withZone(DateTimeZone.getDefault()))
    }

    @Test
    fun `GIVEN transaction without payload WHEN convert THEN null`() {
        // Arrange
        val transaction = TangemPayTxHistoryResponse.Transaction(id = TX_ID, type = "brand_new_type")

        // Act
        val actual = converter.convert(transaction)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN collateral json with cashback kind WHEN parsed and converted THEN cashback collateral item`() {
        // Arrange
        val json = """
            {
              "id": "$TX_ID",
              "type": "collateral",
              "collateral": {
                "amount": 19.00,
                "currency": "usdc",
                "chain_id": 137,
                "wallet_address": "$WALLET_ADDRESS",
                "transaction_hash": "$TX_HASH",
                "posted_at": "2026-08-28T12:08:54.013Z",
                "kind": "CASHBACK"
              }
            }
        """.trimIndent()
        val transaction = requireNotNull(transactionAdapter.fromJson(json))
        val expected = TangemPayTxHistoryItem.Collateral(
            id = TX_ID,
            jsonRepresentation = collateralAdapter.toJson(transaction.collateral),
            date = POSTED_AT.withZone(DateTimeZone.getDefault()),
            amount = BigDecimal("19.00"),
            currency = Currency.getInstance("USD"),
            transactionHash = TX_HASH,
            type = TangemPayTxHistoryItem.Type.Cashback,
        )

        // Act
        val actual = converter.convert(transaction)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN collateral kind and amount WHEN convert THEN type follows kind and falls back to amount sign`(
        model: CollateralTypeModel,
    ) {
        // Arrange
        val transaction = TangemPayTxHistoryResponse.Transaction(
            id = TX_ID,
            type = "collateral",
            collateral = collateral(kind = model.kind, amount = model.amount),
        )

        // Act
        val actual = converter.convert(transaction) as TangemPayTxHistoryItem.Collateral

        // Assert
        assertThat(actual.type).isEqualTo(model.expected)
    }

    data class CollateralTypeModel(
        val kind: String?,
        val amount: BigDecimal,
        val expected: TangemPayTxHistoryItem.Type,
    )

    private fun provideTestModels() = listOf(
        CollateralTypeModel(
            kind = "DEPOSIT",
            amount = BigDecimal("50.00"),
            expected = TangemPayTxHistoryItem.Type.Deposit,
        ),
        CollateralTypeModel(
            kind = "WITHDRAWAL",
            amount = BigDecimal("-20.00"),
            expected = TangemPayTxHistoryItem.Type.Withdrawal,
        ),
        CollateralTypeModel(
            kind = "CASHBACK",
            amount = BigDecimal("19.00"),
            expected = TangemPayTxHistoryItem.Type.Cashback,
        ),
        CollateralTypeModel(
            kind = "cashback",
            amount = BigDecimal("19.00"),
            expected = TangemPayTxHistoryItem.Type.Cashback,
        ),
        CollateralTypeModel(
            kind = null,
            amount = BigDecimal("50.00"),
            expected = TangemPayTxHistoryItem.Type.Deposit,
        ),
        CollateralTypeModel(
            kind = null,
            amount = BigDecimal.ZERO,
            expected = TangemPayTxHistoryItem.Type.Deposit,
        ),
        CollateralTypeModel(
            kind = null,
            amount = BigDecimal("-20.00"),
            expected = TangemPayTxHistoryItem.Type.Withdrawal,
        ),
        CollateralTypeModel(
            kind = "REBATE",
            amount = BigDecimal("50.00"),
            expected = TangemPayTxHistoryItem.Type.Deposit,
        ),
        CollateralTypeModel(
            kind = "REBATE",
            amount = BigDecimal("-20.00"),
            expected = TangemPayTxHistoryItem.Type.Withdrawal,
        ),
    )

    private fun collateral(kind: String?, amount: BigDecimal) = TangemPayTxHistoryResponse.Collateral(
        amount = amount,
        currency = "usdc",
        chainId = 137L,
        walletAddress = WALLET_ADDRESS,
        transactionHash = TX_HASH,
        postedAt = POSTED_AT,
        kind = kind,
    )

    private fun refund(
        amount: BigDecimal = BigDecimal("-15.00"),
        localAmount: BigDecimal? = BigDecimal("-13.50"),
        cashback: BigDecimal? = BigDecimal("-7.50"),
        postedAt: DateTime? = POSTED_AT,
    ) = TangemPayTxHistoryResponse.Refund(
        amount = amount,
        currency = "USD",
        localAmount = localAmount,
        localCurrency = "EUR",
        sourceTransactionId = "fd5861d7-6e9d-42b6-ab7e-9687a861b18e",
        merchantName = "Spotify Premium Renewal",
        merchantCategory = "Entertainment",
        merchantCategoryCode = "5812",
        merchantId = "bb7baadb-9f6d-59d0-af03-00f42243e917",
        enrichedMerchantIcon = "https://example.com/icon.png",
        enrichedMerchantName = "Spotify",
        enrichedMerchantCategory = "Music",
        cardId = "21c88a66-ddc7-4d4c-9bd7-bf5342bf9efc",
        cardType = "virtual",
        cardDisplayName = "Plus Card 1",
        cardNumberEnd = "6460",
        status = "COMPLETED",
        authorizedAt = AUTHORIZED_AT,
        postedAt = postedAt,
        cashback = cashback,
        cashbackStatus = "confirmed",
        cashbackCurrencyCode = "USD",
    )

    private class BigDecimalTestAdapter : JsonAdapter<BigDecimal>() {
        override fun fromJson(reader: JsonReader): BigDecimal = BigDecimal(reader.nextString())
        override fun toJson(writer: JsonWriter, value: BigDecimal?) {
            writer.value(value?.toPlainString())
        }
    }

    private class DateTimeTestAdapter : JsonAdapter<DateTime>() {
        override fun fromJson(reader: JsonReader): DateTime = DateTime.parse(reader.nextString())
        override fun toJson(writer: JsonWriter, value: DateTime?) {
            writer.value(value?.toString())
        }
    }

    private companion object {
        const val TX_ID = "2257c149-33f8-4439-b9b8-57ed85ef35a3"
        const val TX_HASH = "0x5e1c0b0c9f1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f708192a3b4c5d6e7f80910"
        const val WALLET_ADDRESS = "0x8f2b1c3d4e5f60718293a4b5c6d7e8f901a2b3c4"
        val AUTHORIZED_AT: DateTime = DateTime.parse("2026-08-28T12:08:53.939Z")
        val POSTED_AT: DateTime = DateTime.parse("2026-08-28T12:08:54.013Z")
    }
}