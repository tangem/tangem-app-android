package com.tangem.data.txhistory.repository.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.domain.express.models.ExpressAsset
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
        val swap = swapConverter.convert(
            ExpressSwapConverter.Input(
                entity = entity,
                provider = null,
                isOutgoing = true,
                fromCurrency = null,
                toCurrency = null,
                refundCurrency = null,
            ),
        )

        // Assert
        assertThat(swap.isOutgoing).isTrue()
        assertThat(swap.matchHash).isEqualTo("payin")
        assertThat(swap.txInfo).isNull()
        assertThat(swap.tx.status).isEqualTo(ExpressExchangeStatus.Waiting)
        assertThat(swap.createdAtMillis).isEqualTo(DateTime.parse(CREATED_AT).millis)
        // Raw backend amounts are scaled by decimals into the human-readable value the domain model promises.
        assertThat(swap.tx.fromAsset.amount).isEquivalentAccordingToCompareTo(BigDecimal("1.5"))
        assertThat(swap.tx.toAsset.amount).isEquivalentAccordingToCompareTo(BigDecimal("0.001"))
    }

    @Test
    fun `GIVEN exchange entity WHEN toIncomingSwap THEN incoming and matched by payout hash`() {
        // Arrange
        val entity = createExchangeEntity(payinHash = "payin", payoutHash = "payout")

        // Act
        val swap = swapConverter.convert(
            ExpressSwapConverter.Input(
                entity = entity,
                provider = null,
                isOutgoing = false,
                fromCurrency = null,
                toCurrency = null,
                refundCurrency = null,
            ),
        )

        // Assert
        assertThat(swap.isOutgoing).isFalse()
        assertThat(swap.matchHash).isEqualTo("payout")
    }

    @Test
    fun `GIVEN exchange entity with actual amount WHEN toOutgoingSwap THEN to-asset uses actual amount`() {
        // Arrange (raw minimal-unit amounts, to-asset decimals = 8)
        val entity = createExchangeEntity(toAmount = "100000", toActualAmount = "99000")

        // Act
        val swap = swapConverter.convert(
            ExpressSwapConverter.Input(
                entity = entity,
                provider = null,
                isOutgoing = true,
                fromCurrency = null,
                toCurrency = null,
                refundCurrency = null,
            ),
        )

        // Assert
        assertThat(swap.tx.toAsset.amount).isEquivalentAccordingToCompareTo(BigDecimal("0.00099"))
    }

    @Test
    fun `GIVEN exchange entity with refund fields WHEN convert THEN refund asset id mapped to domain`() {
        // Arrange
        val entity = createExchangeEntity(refundNetwork = "polygon", refundContractAddress = "0xrefund")

        // Act
        val swap = swapConverter.convert(
            ExpressSwapConverter.Input(
                entity = entity,
                provider = null,
                isOutgoing = true,
                fromCurrency = null,
                toCurrency = null,
                refundCurrency = null,
            ),
        )

        // Assert
        assertThat(swap.tx.refundAssetId)
            .isEqualTo(ExpressAsset.ID(networkId = "polygon", contractAddress = "0xrefund"))
    }

    @Test
    fun `GIVEN exchange entity without refund fields WHEN convert THEN refund fields are null`() {
        // Act
        val swap = swapConverter.convert(
            ExpressSwapConverter.Input(
                entity = createExchangeEntity(),
                provider = null,
                isOutgoing = true,
                fromCurrency = null,
                toCurrency = null,
                refundCurrency = null,
            ),
        )

        // Assert
        assertThat(swap.tx.refundAssetId).isNull()
        assertThat(swap.tx.refundCurrency).isNull()
    }

    @Test
    fun `GIVEN exchange entity with external tx id WHEN convert THEN external tx id mapped to domain`() {
        // Arrange
        val entity = createExchangeEntity(externalTxId = "ext-1")

        // Act
        val swap = swapConverter.convert(
            ExpressSwapConverter.Input(
                entity = entity,
                provider = null,
                isOutgoing = true,
                fromCurrency = null,
                toCurrency = null,
                refundCurrency = null,
            ),
        )

        // Assert
        assertThat(swap.tx.externalTxId).isEqualTo("ext-1")
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
        assertThat(onramp.tx.fromFiat.value).isEquivalentAccordingToCompareTo(BigDecimal("100"))
        assertThat(onramp.tx.fromFiat.decimals).isEqualTo(2)
        assertThat(onramp.tx.fromFiat.type).isEqualTo(AmountType.FiatType("USD"))
        assertThat(onramp.tx.toAsset.amount).isEquivalentAccordingToCompareTo(BigDecimal("0.5"))
    }

    private fun createExchangeEntity(
        payinHash: String? = "payin",
        payoutHash: String? = "payout",
        status: String = "waiting",
        // Raw minimal-unit amount (to-asset decimals = 8) → 0.001
        toAmount: String = "100000",
        toActualAmount: String? = null,
        refundNetwork: String? = null,
        refundContractAddress: String? = null,
        externalTxId: String? = null,
    ) = ExpressExchangeEntity(
        txId = "tx-1",
        providerId = "provider",
        fromAddress = "owner",
        payinAddress = "payin-addr",
        payinExtraId = null,
        payoutAddress = "payout-addr",
        refundAddress = null,
        refundExtraId = null,
        rateType = "float",
        status = status,
        externalTxId = externalTxId,
        externalTxUrl = "https://ex.url",
        payinHash = payinHash,
        payoutHash = payoutHash,
        refundNetwork = refundNetwork,
        refundContractAddress = refundContractAddress,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
        payTill = null,
        averageDuration = null,
        from = ExpressExchangeEntity.AssetEmbedded(
            contractAddress = "",
            network = "ethereum",
            decimals = 18,
            // Raw minimal-unit amount (decimals = 18) → 1.5
            amount = "1500000000000000000",
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
        // Raw minimal-unit fiat amount (precision = 2) → 100
        fromAmount = "10000",
        fromPrecision = 2,
        to = ExpressOnrampEntity.AssetEmbedded(
            contractAddress = "0xtoken",
            network = "ethereum",
            decimals = 18,
            // Raw minimal-unit amount (decimals = 18) → 0.5
            amount = "500000000000000000",
            actualAmount = null,
        ),
        paymentMethod = "card",
        countryCode = "US",
    )

    private companion object {
        const val CREATED_AT = "2026-06-01T00:00:00Z"
    }
}