package com.tangem.datasource.local.converter

import com.google.common.truth.Truth
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
        Truth.assertThat(entity.txId).isEqualTo(item.txId)
        Truth.assertThat(entity.ownerAddress).isEqualTo(OWNER_ADDRESS)
        Truth.assertThat(entity.providerId).isEqualTo(item.providerId)
        Truth.assertThat(entity.fromAddress).isEqualTo(item.fromAddress)
        Truth.assertThat(entity.payinAddress).isEqualTo(item.payinAddress)
        Truth.assertThat(entity.payinExtraId).isEqualTo(item.payinExtraId)
        Truth.assertThat(entity.payoutAddress).isEqualTo(item.payoutAddress)
        Truth.assertThat(entity.refundAddress).isEqualTo(item.refundAddress)
        Truth.assertThat(entity.refundExtraId).isEqualTo(item.refundExtraId)
        Truth.assertThat(entity.rateType).isEqualTo(item.rateType)
        Truth.assertThat(entity.externalTxId).isEqualTo(item.externalTxId)
        Truth.assertThat(entity.externalTxUrl).isEqualTo(item.externalTxUrl)
        Truth.assertThat(entity.payinHash).isEqualTo(item.payinHash)
        Truth.assertThat(entity.payoutHash).isEqualTo(item.payoutHash)
        Truth.assertThat(entity.refundNetwork).isEqualTo(item.refundNetwork)
        Truth.assertThat(entity.refundContractAddress).isEqualTo(item.refundContractAddress)
        Truth.assertThat(entity.createdAt).isEqualTo(item.createdAt)
        // todo txHistory uncomment
        // Truth.assertThat(entity.updatedAt).isEqualTo(item.updatedAt)
        Truth.assertThat(entity.payTill).isEqualTo(item.payTill)
        Truth.assertThat(entity.averageDuration).isEqualTo(item.averageDuration)
    }

    @Test
    fun `GIVEN exchange item WHEN toEntity THEN status is stored as raw string`() {
        // GIVEN
        val item = createExchangeItem(status = "finished")

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        Truth.assertThat(entity.status).isEqualTo("finished")
    }

    @Test
    fun `GIVEN exchange item WHEN toEntity THEN from and to assets are mapped`() {
        // GIVEN
        val item = createExchangeItem()

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        Truth.assertThat(entity.from.contractAddress).isEqualTo(item.fromContractAddress)
        Truth.assertThat(entity.from.network).isEqualTo(item.fromNetwork)
        Truth.assertThat(entity.from.decimals).isEqualTo(item.fromDecimals)
        Truth.assertThat(entity.from.amount).isEqualTo(item.fromAmount)
        // `from` asset never carries an actual amount
        Truth.assertThat(entity.from.actualAmount).isNull()

        Truth.assertThat(entity.to.contractAddress).isEqualTo(item.toContractAddress)
        Truth.assertThat(entity.to.network).isEqualTo(item.toNetwork)
        Truth.assertThat(entity.to.decimals).isEqualTo(item.toDecimals)
        Truth.assertThat(entity.to.amount).isEqualTo(item.toAmount)
        Truth.assertThat(entity.to.actualAmount).isEqualTo(item.toActualAmount)
    }

    @Test
    fun `GIVEN exchange item with null optional fields WHEN toEntity THEN nulls are preserved`() {
        // GIVEN
        val item = createExchangeItem(
            payinExtraId = null,
            refundAddress = null,
            refundExtraId = null,
            externalTxId = null,
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
        Truth.assertThat(entity.payinExtraId).isNull()
        Truth.assertThat(entity.refundAddress).isNull()
        Truth.assertThat(entity.refundExtraId).isNull()
        Truth.assertThat(entity.externalTxId).isNull()
        Truth.assertThat(entity.externalTxUrl).isNull()
        Truth.assertThat(entity.payinHash).isNull()
        Truth.assertThat(entity.payoutHash).isNull()
        Truth.assertThat(entity.refundNetwork).isNull()
        Truth.assertThat(entity.refundContractAddress).isNull()
        Truth.assertThat(entity.payTill).isNull()
        Truth.assertThat(entity.averageDuration).isNull()
        Truth.assertThat(entity.to.actualAmount).isNull()
    }

    @Test
    fun `GIVEN onramp item WHEN toEntity THEN all transaction fields are mapped`() {
        // GIVEN
        val item = createOnrampItem()

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        Truth.assertThat(entity.txId).isEqualTo(item.txId)
        Truth.assertThat(entity.ownerAddress).isEqualTo(OWNER_ADDRESS)
        Truth.assertThat(entity.providerId).isEqualTo(item.providerId)
        Truth.assertThat(entity.payoutAddress).isEqualTo(item.payoutAddress)
        Truth.assertThat(entity.failReason).isEqualTo(item.failReason)
        Truth.assertThat(entity.externalTxId).isEqualTo(item.externalTxId)
        Truth.assertThat(entity.externalTxUrl).isEqualTo(item.externalTxUrl)
        Truth.assertThat(entity.payoutHash).isEqualTo(item.payoutHash)
        Truth.assertThat(entity.createdAt).isEqualTo(item.createdAt)
        // todo txHistory uncomment
        // Truth.assertThat(entity.updatedAt).isEqualTo(item.updatedAt)
        Truth.assertThat(entity.fromCurrencyCode).isEqualTo(item.fromCurrencyCode)
        Truth.assertThat(entity.fromAmount).isEqualTo(item.fromAmount)
        Truth.assertThat(entity.fromPrecision).isEqualTo(item.fromPrecision)
        Truth.assertThat(entity.paymentMethod).isEqualTo(item.paymentMethod)
        Truth.assertThat(entity.countryCode).isEqualTo(item.countryCode)
    }

    @Test
    fun `GIVEN onramp item WHEN toEntity THEN status is stored as raw string`() {
        // GIVEN
        val item = createOnrampItem(status = "waiting-for-payment")

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        Truth.assertThat(entity.status).isEqualTo("waiting-for-payment")
    }

    @Test
    fun `GIVEN onramp item WHEN toEntity THEN to asset is mapped`() {
        // GIVEN
        val item = createOnrampItem()

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        Truth.assertThat(entity.to.contractAddress).isEqualTo(item.toContractAddress)
        Truth.assertThat(entity.to.network).isEqualTo(item.toNetwork)
        Truth.assertThat(entity.to.decimals).isEqualTo(item.toDecimals)
        Truth.assertThat(entity.to.amount).isEqualTo(item.toAmount)
        Truth.assertThat(entity.to.actualAmount).isEqualTo(item.toActualAmount)
    }

    @Test
    fun `GIVEN onramp item with null optional fields WHEN toEntity THEN nulls are preserved`() {
        // GIVEN
        val item = createOnrampItem(
            failReason = null,
            externalTxId = null,
            externalTxUrl = null,
            payoutHash = null,
            toAmount = null,
            toActualAmount = null,
        )

        // WHEN
        val entity = item.toEntity(ownerAddress = OWNER_ADDRESS)

        // THEN
        Truth.assertThat(entity.failReason).isNull()
        Truth.assertThat(entity.externalTxId).isNull()
        Truth.assertThat(entity.externalTxUrl).isNull()
        Truth.assertThat(entity.payoutHash).isNull()
        Truth.assertThat(entity.to.amount).isNull()
        Truth.assertThat(entity.to.actualAmount).isNull()
    }

    private fun createExchangeItem(
        status: String = "waiting",
        payinExtraId: String? = "payin-extra",
        refundAddress: String? = "refund-address",
        refundExtraId: String? = "refund-extra",
        externalTxId: String? = "external-tx-id",
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
        externalTxUrl = externalTxUrl,
        payinHash = payinHash,
        payoutHash = payoutHash,
        refundNetwork = refundNetwork,
        refundContractAddress = refundContractAddress,
        createdAt = "2026-06-01T00:00:00Z",
        // todo txHistory uncomment
        // updatedAt = "2026-06-01T00:05:00Z",
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
        status: String = "waiting-for-payment",
        failReason: String? = "fail-reason",
        externalTxId: String? = "external-tx-id",
        externalTxUrl: String? = "https://provider.example/tx",
        payoutHash: String? = "payout-hash",
        toAmount: String? = "0.001",
        toActualAmount: String? = "0.00099",
    ) = OnrampItemResponse(
        txId = "onramp-tx-1",
        providerId = "mercuryo",
        payoutAddress = "0xpayout",
        status = status,
        failReason = failReason,
        externalTxId = externalTxId,
        externalTxUrl = externalTxUrl,
        payoutHash = payoutHash,
        createdAt = "2026-06-01T00:00:00Z",
        // todo txHistory uncomment
        // updatedAt = "2026-06-01T00:05:00Z",
        fromCurrencyCode = "USD",
        fromAmount = "100.0",
        fromPrecision = 2,
        toContractAddress = "0xtoContract",
        toNetwork = "bitcoin",
        toDecimals = 8,
        toAmount = toAmount,
        toActualAmount = toActualAmount,
        paymentMethod = "card",
        countryCode = "US",
    )

    private companion object {
        const val OWNER_ADDRESS = "0xowner"
    }
}