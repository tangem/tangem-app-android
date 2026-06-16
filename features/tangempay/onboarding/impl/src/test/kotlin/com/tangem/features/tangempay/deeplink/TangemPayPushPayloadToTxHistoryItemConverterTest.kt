package com.tangem.features.tangempay.deeplink

import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TangemPayPushPayloadToTxHistoryItemConverterTest {

    @Test
    fun `convertSpend returns Spend tx with all fields when payload is complete`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "local_amount" to "5.24",
            "local_currency" to "usd",
            "authorized_amount" to "5.24",
            "merchant_name" to "PLAYSTATION NETWORK",
            "enriched_merchant_name" to "Playstation",
            "enriched_merchant_icon" to "https://example.com/icon.png",
            "enriched_merchant_category" to "Gaming",
            "merchant_category" to "Digital Goods",
            "merchant_category_code" to "5818",
            "status" to "completed",
            "declined_reason" to "",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("txn-123")
        assertThat(result.amount).isEqualTo(BigDecimal("5.24"))
        assertThat(result.currency.currencyCode).isEqualTo("USD")
        assertThat(result.localAmount).isEqualTo(BigDecimal("5.24"))
        assertThat(result.localCurrency?.currencyCode).isEqualTo("USD")
        assertThat(result.authorizedAmount).isEqualTo(BigDecimal("5.24"))
        assertThat(result.merchantName).isEqualTo("PLAYSTATION NETWORK")
        assertThat(result.enrichedMerchantName).isEqualTo("Playstation")
        assertThat(result.enrichedMerchantIconUrl).isEqualTo("https://example.com/icon.png")
        assertThat(result.enrichedMerchantCategory).isEqualTo("Gaming")
        assertThat(result.merchantCategory).isEqualTo("Digital Goods")
        assertThat(result.merchantCategoryCode).isEqualTo("5818")
        assertThat(result.status).isEqualTo(TangemPayTxHistoryItem.Status.COMPLETED)
    }

    @Test
    fun `convertSpend returns null when transaction_id is missing`() {
        val payload = mapOf(
            "amount" to "5.24",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when amount is missing`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when currency is missing`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when merchant_name is missing`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend falls back to enriched_merchant_name when merchant_name is absent`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "enriched_merchant_name" to "Playstation",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNotNull()
        assertThat(result!!.merchantName).isEqualTo("Playstation")
    }

    @Test
    fun `convertSpend returns null when status is missing`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "merchant_name" to "Test",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when authorized_at is missing`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when amount is not a number`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "abc",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend returns null when currency is invalid`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "INVALID",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend maps all statuses correctly`() {
        fun spendPayloadWithStatus(status: String) = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "1.00",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to status,
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("pending"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.PENDING)
        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("reserved"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.RESERVED)
        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("completed"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.COMPLETED)
        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("declined"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.DECLINED)
        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("reversed"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.REVERSED)
        assertThat(TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(spendPayloadWithStatus("unknown_value"))!!.status)
            .isEqualTo(TangemPayTxHistoryItem.Status.UNKNOWN)
    }

    @Test
    fun `convertSpend returns null when transaction_id is empty`() {
        val payload = mapOf(
            "transaction_id" to "",
            "amount" to "5.24",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertCollateral returns Collateral with all fields when payload is complete`() {
        val payload = mapOf(
            "transaction_id" to "col-123",
            "amount" to "50.00",
            "currency" to "usd",
            "transaction_hash" to "0xabc123",
            "posted_at" to "2025-10-25T19:22:22.597Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("col-123")
        assertThat(result.amount).isEqualTo(BigDecimal("50.00"))
        assertThat(result.currency.currencyCode).isEqualTo("USD")
        assertThat(result.transactionHash).isEqualTo("0xabc123")
        assertThat(result.type).isEqualTo(TangemPayTxHistoryItem.Type.Deposit)
    }

    @Test
    fun `convertCollateral returns Withdrawal type for negative amount`() {
        val payload = mapOf(
            "transaction_id" to "col-456",
            "amount" to "-10.00",
            "currency" to "usd",
            "transaction_hash" to "0xdef789",
            "posted_at" to "2025-10-25T19:22:22.597Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        assertThat(result).isNotNull()
        assertThat(result!!.type).isEqualTo(TangemPayTxHistoryItem.Type.Withdrawal)
    }

    @Test
    fun `convertCollateral returns null when transaction_id is missing`() {
        val payload = mapOf(
            "amount" to "50.00",
            "currency" to "usd",
            "transaction_hash" to "0xabc123",
            "posted_at" to "2025-10-25T19:22:22.597Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertCollateral returns null when amount is missing`() {
        val payload = mapOf(
            "transaction_id" to "col-123",
            "currency" to "usd",
            "transaction_hash" to "0xabc123",
            "posted_at" to "2025-10-25T19:22:22.597Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertCollateral returns null when transaction_hash is missing`() {
        val payload = mapOf(
            "transaction_id" to "col-123",
            "amount" to "50.00",
            "currency" to "usd",
            "posted_at" to "2025-10-25T19:22:22.597Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertCollateral returns null when posted_at is missing`() {
        val payload = mapOf(
            "transaction_id" to "col-123",
            "amount" to "50.00",
            "currency" to "usd",
            "transaction_hash" to "0xabc123",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertCollateral returns null when currency is missing`() {
        val payload = mapOf(
            "transaction_id" to "col-123",
            "amount" to "50.00",
            "transaction_hash" to "0xabc123",
            "posted_at" to "2025-10-25T19:22:22.597Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertCollateral returns null when transaction_hash is empty`() {
        val payload = mapOf(
            "transaction_id" to "col-123",
            "amount" to "50.00",
            "currency" to "usd",
            "transaction_hash" to "",
            "posted_at" to "2025-10-25T19:22:22.597Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        assertThat(result).isNull()
    }

    @Test
    fun `convertSpend handles optional fields as null`() {
        val payload = mapOf(
            "transaction_id" to "txn-123",
            "amount" to "5.24",
            "currency" to "usd",
            "merchant_name" to "Test",
            "status" to "completed",
            "authorized_at" to "2025-10-24T10:32:24.496Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)

        assertThat(result).isNotNull()
        assertThat(result!!.localAmount).isNull()
        assertThat(result.localCurrency).isNull()
        assertThat(result.enrichedMerchantName).isNull()
        assertThat(result.enrichedMerchantIconUrl).isNull()
        assertThat(result.enrichedMerchantCategory).isNull()
        assertThat(result.merchantCategory).isNull()
        assertThat(result.merchantCategoryCode).isNull()
        assertThat(result.declinedReason).isNull()
    }

    @Test
    fun `convertCollateral returns Deposit type for zero amount`() {
        val payload = mapOf(
            "transaction_id" to "col-789",
            "amount" to "0",
            "currency" to "usd",
            "transaction_hash" to "0xdef",
            "posted_at" to "2025-10-25T19:22:22.597Z",
        )

        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)

        assertThat(result).isNotNull()
        assertThat(result!!.type).isEqualTo(TangemPayTxHistoryItem.Type.Deposit)
    }

    @Test
    fun `convertSpend returns null for empty payload`() {
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(emptyMap())
        assertThat(result).isNull()
    }

    @Test
    fun `convertCollateral returns null for empty payload`() {
        val result = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(emptyMap())
        assertThat(result).isNull()
    }
}
