package com.tangem.data.visa.utils

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.spend.datasource.pay.models.response.TangemPayTxHistoryResponse
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

internal class TangemPayTxHistoryItemConverterTest {

    private val moshi = Moshi.Builder()
        .add(BigDecimal::class.java, BigDecimalTestAdapter())
        .add(DateTime::class.java, DateTimeTestAdapter())
        .build()

    private val refundAdapter = moshi.adapter(TangemPayTxHistoryResponse.Refund::class.java)

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
        val AUTHORIZED_AT: DateTime = DateTime.parse("2026-08-28T12:08:53.939Z")
        val POSTED_AT: DateTime = DateTime.parse("2026-08-28T12:08:54.013Z")
    }
}