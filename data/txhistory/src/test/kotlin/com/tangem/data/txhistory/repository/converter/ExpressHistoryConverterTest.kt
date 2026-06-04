package com.tangem.data.txhistory.repository.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.express.models.response.ExchangeItemResponse
import com.tangem.datasource.api.onramp.models.response.OnrampItemResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExpressHistoryConverterTest {

    @Test
    fun `GIVEN exchange item WHEN toEntity THEN all transaction fields are mapped`() {
        // GIVEN
        val item = createExchangeItem()

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        assertThat(entity.txId).isEqualTo(item.txId)
        assertThat(entity.ownerAddress).isEqualTo(OWNER_ADDRESS)
        assertThat(entity.providerId).isEqualTo(item.providerId)
        assertThat(entity.fromAddress).isEqualTo(item.fromAddress)
        assertThat(entity.payinAddress).isEqualTo(item.payinAddress)
        assertThat(entity.payinExtraId).isEqualTo(item.payinExtraId)
        assertThat(entity.payoutAddress).isEqualTo(item.payoutAddress)
        assertThat(entity.refundAddress).isEqualTo(item.refundAddress)
        assertThat(entity.refundExtraId).isEqualTo(item.refundExtraId)
        assertThat(entity.rateType).isEqualTo(item.rateType)
        assertThat(entity.externalTxId).isEqualTo(item.externalTxId)
        assertThat(entity.externalTxStatus).isEqualTo(item.externalTxStatus)
        assertThat(entity.externalTxUrl).isEqualTo(item.externalTxUrl)
        assertThat(entity.payinHash).isEqualTo(item.payinHash)
        assertThat(entity.payoutHash).isEqualTo(item.payoutHash)
        assertThat(entity.refundNetwork).isEqualTo(item.refundNetwork)
        assertThat(entity.refundContractAddress).isEqualTo(item.refundContractAddress)
        assertThat(entity.createdAt).isEqualTo(item.createdAt)
        assertThat(entity.payTill).isEqualTo(item.payTill)
        assertThat(entity.averageDuration).isEqualTo(item.averageDuration)
    }

    @Test
    fun `GIVEN exchange item WHEN toEntity THEN status is stored as enum name`() {
        // GIVEN
        val item = createExchangeItem(status = ExchangeItemResponse.Status.FINISHED)

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        assertThat(entity.status).isEqualTo("FINISHED")
    }

    @Test
    fun `GIVEN exchange item WHEN toEntity THEN from and to assets are mapped`() {
        // GIVEN
        val item = createExchangeItem()

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        assertThat(entity.from.contractAddress).isEqualTo(item.fromContractAddress)
        assertThat(entity.from.network).isEqualTo(item.fromNetwork)
        assertThat(entity.from.decimals).isEqualTo(item.fromDecimals)
        assertThat(entity.from.amount).isEqualTo(item.fromAmount)
        // `from` asset never carries an actual amount
        assertThat(entity.from.actualAmount).isNull()

        assertThat(entity.to.contractAddress).isEqualTo(item.toContractAddress)
        assertThat(entity.to.network).isEqualTo(item.toNetwork)
        assertThat(entity.to.decimals).isEqualTo(item.toDecimals)
        assertThat(entity.to.amount).isEqualTo(item.toAmount)
        assertThat(entity.to.actualAmount).isEqualTo(item.toActualAmount)
    }

    @Test
    fun `GIVEN exchange item with null optional fields WHEN toEntity THEN nulls are preserved`() {
        // GIVEN
        val item = createExchangeItem(
            payinExtraId = null,
            refundAddress = null,
            refundExtraId = null,
            externalTxId = null,
            externalTxStatus = null,
            externalTxUrl = null,
            payinHash = null,
            payoutHash = null,
            refundNetwork = null,
            refundContractAddress = null,
            payTill = null,
            averageDuration = null,
            toActualAmount = null,
        )

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        assertThat(entity.payinExtraId).isNull()
        assertThat(entity.refundAddress).isNull()
        assertThat(entity.refundExtraId).isNull()
        assertThat(entity.externalTxId).isNull()
        assertThat(entity.externalTxStatus).isNull()
        assertThat(entity.externalTxUrl).isNull()
        assertThat(entity.payinHash).isNull()
        assertThat(entity.payoutHash).isNull()
        assertThat(entity.refundNetwork).isNull()
        assertThat(entity.refundContractAddress).isNull()
        assertThat(entity.payTill).isNull()
        assertThat(entity.averageDuration).isNull()
        assertThat(entity.to.actualAmount).isNull()
    }

    @Test
    fun `GIVEN onramp item WHEN toEntity THEN all transaction fields are mapped`() {
        // GIVEN
        val item = createOnrampItem()

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        assertThat(entity.txId).isEqualTo(item.txId)
        assertThat(entity.ownerAddress).isEqualTo(OWNER_ADDRESS)
        assertThat(entity.providerId).isEqualTo(item.providerId)
        assertThat(entity.fromAddress).isEqualTo(item.fromAddress)
        assertThat(entity.payinAddress).isEqualTo(item.payinAddress)
        assertThat(entity.payinExtraId).isEqualTo(item.payinExtraId)
        assertThat(entity.payoutAddress).isEqualTo(item.payoutAddress)
        assertThat(entity.refundAddress).isEqualTo(item.refundAddress)
        assertThat(entity.refundExtraId).isEqualTo(item.refundExtraId)
        assertThat(entity.rateType).isEqualTo(item.rateType)
        assertThat(entity.externalTxId).isEqualTo(item.externalTxId)
        assertThat(entity.externalTxStatus).isEqualTo(item.externalTxStatus)
        assertThat(entity.externalTxUrl).isEqualTo(item.externalTxUrl)
        assertThat(entity.payinHash).isEqualTo(item.payinHash)
        assertThat(entity.payoutHash).isEqualTo(item.payoutHash)
        assertThat(entity.refundNetwork).isEqualTo(item.refundNetwork)
        assertThat(entity.refundContractAddress).isEqualTo(item.refundContractAddress)
        assertThat(entity.createdAt).isEqualTo(item.createdAt)
        assertThat(entity.payTill).isEqualTo(item.payTill)
        assertThat(entity.averageDuration).isEqualTo(item.averageDuration)
    }

    @Test
    fun `GIVEN onramp item WHEN toEntity THEN status is stored as enum name`() {
        // GIVEN
        val item = createOnrampItem(status = OnrampItemResponse.Status.WAITING_TX_HASH)

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        assertThat(entity.status).isEqualTo("WAITING_TX_HASH")
    }

    @Test
    fun `GIVEN onramp item WHEN toEntity THEN from and to assets are mapped`() {
        // GIVEN
        val item = createOnrampItem()

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        assertThat(entity.from.contractAddress).isEqualTo(item.fromContractAddress)
        assertThat(entity.from.network).isEqualTo(item.fromNetwork)
        assertThat(entity.from.decimals).isEqualTo(item.fromDecimals)
        assertThat(entity.from.amount).isEqualTo(item.fromAmount)
        assertThat(entity.from.actualAmount).isNull()

        assertThat(entity.to.contractAddress).isEqualTo(item.toContractAddress)
        assertThat(entity.to.network).isEqualTo(item.toNetwork)
        assertThat(entity.to.decimals).isEqualTo(item.toDecimals)
        assertThat(entity.to.amount).isEqualTo(item.toAmount)
        assertThat(entity.to.actualAmount).isEqualTo(item.toActualAmount)
    }

    @Test
    fun `GIVEN onramp item with null optional fields WHEN toEntity THEN nulls are preserved`() {
        // GIVEN
        val item = createOnrampItem(
            payinExtraId = null,
            refundAddress = null,
            refundExtraId = null,
            externalTxId = null,
            externalTxStatus = null,
            externalTxUrl = null,
            payinHash = null,
            payoutHash = null,
            refundNetwork = null,
            refundContractAddress = null,
            payTill = null,
            averageDuration = null,
            toActualAmount = null,
        )

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        assertThat(entity.payinExtraId).isNull()
        assertThat(entity.refundAddress).isNull()
        assertThat(entity.refundExtraId).isNull()
        assertThat(entity.externalTxId).isNull()
        assertThat(entity.externalTxStatus).isNull()
        assertThat(entity.externalTxUrl).isNull()
        assertThat(entity.payinHash).isNull()
        assertThat(entity.payoutHash).isNull()
        assertThat(entity.refundNetwork).isNull()
        assertThat(entity.refundContractAddress).isNull()
        assertThat(entity.payTill).isNull()
        assertThat(entity.averageDuration).isNull()
        assertThat(entity.to.actualAmount).isNull()
    }

    private fun createExchangeItem(
        status: ExchangeItemResponse.Status = ExchangeItemResponse.Status.WAITING,
        payinExtraId: String? = "payin-extra",
        refundAddress: String? = "refund-address",
        refundExtraId: String? = "refund-extra",
        externalTxId: String? = "external-tx-id",
        externalTxStatus: String? = "external-status",
        externalTxUrl: String? = "https://provider.example/tx",
        payinHash: String? = "payin-hash",
        payoutHash: String? = "payout-hash",
        refundNetwork: String? = "ethereum",
        refundContractAddress: String? = "0xrefund",
        payTill: String? = "2026-06-01T00:10:00Z",
        averageDuration: Long? = 600L,
        toActualAmount: String? = "0.99",
    ) = ExchangeItemResponse(
        txId = "exchange-tx-1",
        providerId = "changelly",
        fromAddress = "0xfrom",
        payinAddress = "0xpayin",
        payinExtraId = payinExtraId,
        payoutAddress = "0xpayout",
        refundAddress = refundAddress,
        refundExtraId = refundExtraId,
        rateType = "float",
        status = status,
        externalTxId = externalTxId,
        externalTxStatus = externalTxStatus,
        externalTxUrl = externalTxUrl,
        payinHash = payinHash,
        payoutHash = payoutHash,
        refundNetwork = refundNetwork,
        refundContractAddress = refundContractAddress,
        createdAt = "2026-06-01T00:00:00Z",
        payTill = payTill,
        averageDuration = averageDuration,
        fromContractAddress = "0xfromContract",
        fromNetwork = "ethereum",
        fromDecimals = 18,
        fromAmount = "1.0",
        toContractAddress = "0xtoContract",
        toNetwork = "bitcoin",
        toDecimals = 8,
        toAmount = "1.0",
        toActualAmount = toActualAmount,
    )

    private fun createOnrampItem(
        status: OnrampItemResponse.Status = OnrampItemResponse.Status.WAITING,
        payinExtraId: String? = "payin-extra",
        refundAddress: String? = "refund-address",
        refundExtraId: String? = "refund-extra",
        externalTxId: String? = "external-tx-id",
        externalTxStatus: String? = "external-status",
        externalTxUrl: String? = "https://provider.example/tx",
        payinHash: String? = "payin-hash",
        payoutHash: String? = "payout-hash",
        refundNetwork: String? = "ethereum",
        refundContractAddress: String? = "0xrefund",
        payTill: String? = "2026-06-01T00:10:00Z",
        averageDuration: Long? = 600L,
        toActualAmount: String? = "0.99",
    ) = OnrampItemResponse(
        txId = "onramp-tx-1",
        providerId = "mercuryo",
        fromAddress = "0xfrom",
        payinAddress = "0xpayin",
        payinExtraId = payinExtraId,
        payoutAddress = "0xpayout",
        refundAddress = refundAddress,
        refundExtraId = refundExtraId,
        rateType = "fixed",
        status = status,
        externalTxId = externalTxId,
        externalTxStatus = externalTxStatus,
        externalTxUrl = externalTxUrl,
        payinHash = payinHash,
        payoutHash = payoutHash,
        refundNetwork = refundNetwork,
        refundContractAddress = refundContractAddress,
        createdAt = "2026-06-01T00:00:00Z",
        payTill = payTill,
        averageDuration = averageDuration,
        fromContractAddress = "0xfromContract",
        fromNetwork = "usd",
        fromDecimals = 2,
        fromAmount = "100.0",
        toContractAddress = "0xtoContract",
        toNetwork = "bitcoin",
        toDecimals = 8,
        toAmount = "0.001",
        toActualAmount = toActualAmount,
    )

    private companion object {
        const val OWNER_ADDRESS = "0xowner"
    }
}