package com.tangem.data.txhistory.repository.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.tokens.model.AmountType
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExpressTxHistoryConverterTest {

    private val swapConverter = ExpressSwapConverter()
    private val onrampConverter = ExpressOnrampConverter()

    @Test
    fun `GIVEN exchange entity WHEN toOutgoingSwap THEN outgoing and matched by payin hash`() {
        // Arrange
        val entity = createExchangeEntity(payinHash = "payin", payoutHash = "payout", status = "waiting")

        // Act
        val swap = swapConverter.convert(ExpressSwapConverter.Input(entity, provider = null, isOutgoing = true))

        // Assert
        assertThat(swap.isOutgoing).isTrue()
        assertThat(swap.matchHash).isEqualTo("payin")
        assertThat(swap.txInfo).isNull()
        assertThat(swap.tx.status).isEqualTo(ExpressExchangeStatus.Waiting)
        assertThat(swap.createdAtMillis).isEqualTo(DateTime.parse(CREATED_AT).millis)
        assertThat(swap.tx.fromAsset.amount).isEqualTo(BigDecimal("1.5"))
        assertThat(swap.tx.toAsset.amount).isEqualTo(BigDecimal("0.001"))
    }

    @Test
    fun `GIVEN exchange entity WHEN toIncomingSwap THEN incoming and matched by payout hash`() {
        // Arrange
        val entity = createExchangeEntity(payinHash = "payin", payoutHash = "payout")

        // Act
        val swap = swapConverter.convert(ExpressSwapConverter.Input(entity, provider = null, isOutgoing = false))

        // Assert
        assertThat(swap.isOutgoing).isFalse()
        assertThat(swap.matchHash).isEqualTo("payout")
    }

    @Test
    fun `GIVEN exchange entity with actual amount WHEN toOutgoingSwap THEN to-asset uses actual amount`() {
        // Arrange
        val entity = createExchangeEntity(toAmount = "0.001", toActualAmount = "0.00099")

        // Act
        val swap = swapConverter.convert(ExpressSwapConverter.Input(entity, provider = null, isOutgoing = true))

        // Assert
        assertThat(swap.tx.toAsset.amount).isEqualTo(BigDecimal("0.00099"))
    }

    @Test
    fun `GIVEN onramp entity WHEN toOnramp THEN matched by payout hash with fiat from-leg`() {
        // Arrange
        val entity = createOnrampEntity(payoutHash = "payout", status = "finished")

        // Act
        val onramp = onrampConverter.convert(ExpressOnrampConverter.Input(entity, provider = null))

        // Assert
        assertThat(onramp.matchHash).isEqualTo("payout")
        assertThat(onramp.txInfo).isNull()
        assertThat(onramp.tx.status).isEqualTo(ExpressOnrampStatus.Finished)
        assertThat(onramp.tx.fromFiat.currencySymbol).isEqualTo("USD")
        assertThat(onramp.tx.fromFiat.value).isEqualTo(BigDecimal("100.0"))
        assertThat(onramp.tx.fromFiat.decimals).isEqualTo(2)
        assertThat(onramp.tx.fromFiat.type).isEqualTo(AmountType.FiatType("USD"))
        assertThat(onramp.tx.toAsset.amount).isEqualTo(BigDecimal("0.5"))
    }

    private fun createExchangeEntity(
        payinHash: String? = "payin",
        payoutHash: String? = "payout",
        status: String = "waiting",
        toAmount: String = "0.001",
        toActualAmount: String? = null,
    ) = ExpressExchangeEntity(
        txId = "tx-1",
        ownerAddress = "owner",
        providerId = "provider",
        fromAddress = "owner",
        payinAddress = "payin-addr",
        payinExtraId = null,
        payoutAddress = "payout-addr",
        refundAddress = null,
        refundExtraId = null,
        rateType = "float",
        status = status,
        externalTxId = null,
        externalTxUrl = "https://ex.url",
        payinHash = payinHash,
        payoutHash = payoutHash,
        refundNetwork = null,
        refundContractAddress = null,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
        payTill = null,
        averageDuration = null,
        from = ExpressExchangeEntity.AssetEmbedded(
            contractAddress = "",
            network = "ethereum",
            decimals = 18,
            amount = "1.5",
            actualAmount = null,
        ),
        to = ExpressExchangeEntity.AssetEmbedded(
            contractAddress = "0xtoken",
            network = "bitcoin",
            decimals = 8,
            amount = toAmount,
            actualAmount = toActualAmount,
        ),
    )

    private fun createOnrampEntity(
        payoutHash: String? = "payout",
        status: String = "finished",
    ) = ExpressOnrampEntity(
        txId = "onramp-1",
        ownerAddress = "owner",
        providerId = "provider",
        payoutAddress = "owner",
        status = status,
        failReason = null,
        externalTxId = null,
        externalTxUrl = null,
        payoutHash = payoutHash,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
        fromCurrencyCode = "USD",
        fromAmount = "100.0",
        fromPrecision = 2,
        to = ExpressOnrampEntity.AssetEmbedded(
            contractAddress = "0xtoken",
            network = "ethereum",
            decimals = 18,
            amount = "0.5",
            actualAmount = null,
        ),
        paymentMethod = "card",
        countryCode = "US",
    )

    private companion object {
        const val CREATED_AT = "2026-06-01T00:00:00Z"
    }
}