package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ExpressTxToDetailsUMConverterTest : TxDetailsConverterTestBase() {

    private val converter = expressConverter()

    // region Header / dispatch

    @Test
    fun `GIVEN express swap WHEN convert THEN TwoAssets with exchange icon`() {
        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Exchanging))

        // Assert
        assertThat(result.header.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20))
    }

    @Test
    fun `GIVEN express onramp WHEN convert THEN TwoAssets with card icon`() {
        // Act
        val result = converter.convert(expressOnramp(status = ExpressOnrampStatus.Finished))

        // Assert
        assertThat(result.header.icon).isEqualTo(TxIcon.Vector(Icons.ic_card_20))
    }

    // endregion

    // region Status banners

    @Test
    fun `GIVEN exchanging express swap WHEN convert THEN info status banner with loader`() {
        // Act
        val banner = converter.convert(expressSwap(status = ExpressExchangeStatus.Exchanging)).statusBanner

        // Assert
        assertThat(banner).isEqualTo(
            TxHistoryDetailsUM.StatusBannerUM(
                severity = TxHistoryDetailsUM.StatusBannerUM.Severity.Info,
                title = resourceReference(R.string.express_exchange_status_exchanging_active),
                isLoading = true,
            ),
        )
    }

    @Test
    fun `GIVEN verifying express swap WHEN convert THEN warning status banner with verification subtitle`() {
        // Act
        val banner = converter.convert(expressSwap(status = ExpressExchangeStatus.Verifying)).statusBanner

        // Assert
        assertThat(banner).isEqualTo(
            TxHistoryDetailsUM.StatusBannerUM(
                severity = TxHistoryDetailsUM.StatusBannerUM.Severity.Warning,
                title = resourceReference(R.string.express_exchange_status_verifying),
                subtitle = resourceReference(R.string.express_exchange_notification_verification_text),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `GIVEN finished express swap WHEN convert THEN success status banner`() {
        // Act
        val banner = converter.convert(expressSwap(status = ExpressExchangeStatus.Finished)).statusBanner

        // Assert
        assertThat(banner).isEqualTo(
            TxHistoryDetailsUM.StatusBannerUM(
                severity = TxHistoryDetailsUM.StatusBannerUM.Severity.Success,
                title = resourceReference(R.string.express_exchange_status_exchanged),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `GIVEN unknown express swap WHEN convert THEN no status banner`() {
        // Act — nothing to surface, the plaque is hidden.
        val banner = converter.convert(expressSwap(status = ExpressExchangeStatus.Unknown)).statusBanner

        // Assert
        assertThat(banner).isNull()
    }

    @Test
    fun `GIVEN failed express swap WHEN convert THEN error status banner with refund subtitle`() {
        // Act
        val banner = converter.convert(expressSwap(status = ExpressExchangeStatus.Failed)).statusBanner

        // Assert
        assertThat(banner).isEqualTo(
            TxHistoryDetailsUM.StatusBannerUM(
                severity = TxHistoryDetailsUM.StatusBannerUM.Severity.Error,
                title = resourceReference(R.string.express_exchange_status_failed),
                subtitle = resourceReference(R.string.express_exchange_notification_failed_text),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `GIVEN finished express onramp WHEN convert THEN success banner`() {
        // Act
        val banner = converter.convert(expressOnramp(status = ExpressOnrampStatus.Finished)).statusBanner

        // Assert
        assertThat(banner).isEqualTo(
            TxHistoryDetailsUM.StatusBannerUM(
                severity = TxHistoryDetailsUM.StatusBannerUM.Severity.Success,
                title = resourceReference(R.string.express_exchange_status_bought),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `GIVEN verifying express onramp WHEN convert THEN warning status banner with verification subtitle`() {
        // Act
        val banner = converter.convert(expressOnramp(status = ExpressOnrampStatus.Verifying)).statusBanner

        // Assert
        assertThat(banner).isEqualTo(
            TxHistoryDetailsUM.StatusBannerUM(
                severity = TxHistoryDetailsUM.StatusBannerUM.Severity.Warning,
                title = resourceReference(R.string.express_exchange_status_verifying),
                subtitle = resourceReference(R.string.express_exchange_notification_verification_text),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `GIVEN unknown express onramp WHEN convert THEN no status banner`() {
        // Act — nothing to surface, the plaque is hidden.
        val banner = converter.convert(expressOnramp(status = ExpressOnrampStatus.Unknown)).statusBanner

        // Assert
        assertThat(banner).isNull()
    }

    // endregion

    // region Asset legs

    @Test
    fun `GIVEN in-progress express swap WHEN convert THEN from is minus and to is approx, neither faded`() {
        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Exchanging))

        // Assert
        assertThat(result.from?.amount?.resolveString()).startsWith("- ")
        assertThat(result.from?.isFaded).isFalse()
        // Receive amount is still an estimate while in flight: `~`, not `+`, and not struck through.
        assertThat(result.to?.amount?.resolveString()).startsWith("~ ")
        assertThat(result.to?.isFaded).isFalse()
        // Counterparty (to) symbol comes from the resolved CryptoCurrency; the unresolved from leg falls back to network id.
        assertThat(result.to?.currencyIcon).isNotNull()
        assertThat(result.from?.currencyIcon).isNull()
    }

    @Test
    fun `GIVEN finished express swap WHEN convert THEN to is plus and neither leg faded`() {
        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Finished))

        // Assert
        assertThat(result.from?.amount?.resolveString()).startsWith("- ")
        assertThat(result.to?.amount?.resolveString()).startsWith("+ ")
        assertThat(result.from?.isFaded).isFalse()
        assertThat(result.to?.isFaded).isFalse()
    }

    @Test
    fun `GIVEN failed express swap WHEN convert THEN from keeps minus unfaded and to is faded unsigned`() {
        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Failed))

        // Assert — the spent leg stands as sent; only the never-received leg is struck through, with no sign.
        assertThat(result.from?.amount?.resolveString()).startsWith("- ")
        assertThat(result.from?.isFaded).isFalse()
        assertThat(result.to?.isFaded).isTrue()
        assertThat(result.to?.amount?.resolveString()).doesNotContain("+")
        assertThat(result.to?.amount?.resolveString()).doesNotContain("~")
    }

    @Test
    fun `GIVEN refunded express swap WHEN convert THEN from keeps minus unfaded and to is faded unsigned`() {
        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Refunded))

        // Assert
        assertThat(result.from?.amount?.resolveString()).startsWith("- ")
        assertThat(result.from?.isFaded).isFalse()
        assertThat(result.to?.isFaded).isTrue()
        assertThat(result.to?.amount?.resolveString()).doesNotContain("+")
        assertThat(result.to?.amount?.resolveString()).doesNotContain("~")
    }

    @Test
    fun `GIVEN finished express onramp WHEN convert THEN paid fiat is unsigned and topped-up crypto is plus`() {
        // Act
        val result = converter.convert(expressOnramp(status = ExpressOnrampStatus.Finished))

        // Assert
        // "You paid" fiat carries no icon and no sign — the exact amount paid.
        assertThat(result.from?.currencyIcon).isNull()
        assertThat(result.from?.amount?.resolveString()).contains("SEK")
        assertThat(result.from?.amount?.resolveString()).doesNotContain("-")
        assertThat(result.from?.amount?.resolveString()).doesNotContain("+")
        assertThat(result.from?.amount?.resolveString()).doesNotContain("~")
        // Topped-up crypto leg is settled: `+`, with an icon.
        assertThat(result.to?.currencyIcon).isNotNull()
        assertThat(result.to?.amount?.resolveString()).startsWith("+ ")
        assertThat(result.to?.isFaded).isFalse()
    }

    @Test
    fun `GIVEN in-progress express onramp WHEN convert THEN paid fiat is unsigned and top-up crypto is approx`() {
        // Act
        val result = converter.convert(expressOnramp(status = ExpressOnrampStatus.Sending))

        // Assert
        // "You paid" stays unsigned regardless of status.
        assertThat(result.from?.amount?.resolveString()).doesNotContain("-")
        assertThat(result.from?.amount?.resolveString()).doesNotContain("+")
        assertThat(result.from?.amount?.resolveString()).doesNotContain("~")
        assertThat(result.from?.isFaded).isFalse()
        // Crypto to-be-received is an estimate while in flight: `~`, not struck through.
        assertThat(result.to?.amount?.resolveString()).startsWith("~ ")
        assertThat(result.to?.isFaded).isFalse()
    }

    // endregion

    // region Info rows (provider / rate / network fee)

    @Test
    fun `GIVEN express swap with matched on-chain leg WHEN convert THEN network-fee row from leg`() {
        // Arrange
        val leg = onChain(
            type = TransactionType.Swap,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Finished, txInfo = leg))

        // Assert — no provider in the fixture, so rate then the on-chain leg's network fee.
        assertThat(result.rows.map { it.label }).containsExactly(
            resourceReference(R.string.common_rate),
            resourceReference(R.string.common_network_fee_title),
        ).inOrder()
    }

    @Test
    fun `GIVEN express swap with provider and url WHEN convert THEN provider row links to the url`() {
        // Act
        val result = converter.convert(
            expressSwap(
                status = ExpressExchangeStatus.Finished,
                provider = provider(name = "Mercuryo"),
                externalTxUrl = EXTERNAL_URL,
            ),
        )

        // Assert — provider then rate (no on-chain leg, so no fee row).
        assertThat(result.rows.map { it.label }).containsExactly(
            resourceReference(R.string.express_provider),
            resourceReference(R.string.common_rate),
        ).inOrder()
        val providerRow = result.rows.first()
        assertThat(providerRow.value.resolveString()).isEqualTo("Mercuryo")
        assertThat(providerRow.trailingIconRes).isEqualTo(R.drawable.ic_arrow_top_right_24)
        providerRow.onClick?.invoke()
        assertThat(openedUrls).containsExactly(EXTERNAL_URL)
    }

    @Test
    fun `GIVEN express swap with provider but no url WHEN convert THEN provider row has no link`() {
        // Act — the provider supplies no link (e.g. DEX), so the row is plain text.
        val result = converter.convert(
            expressSwap(status = ExpressExchangeStatus.Finished, provider = provider(name = "Mercuryo")),
        )

        // Assert
        val providerRow = result.rows.first()
        assertThat(providerRow.value.resolveString()).isEqualTo("Mercuryo")
        assertThat(providerRow.trailingIconRes).isNull()
        assertThat(providerRow.onClick).isNull()
    }

    @Test
    fun `GIVEN express swap with provider and on-chain leg WHEN convert THEN provider row precedes network-fee row`() {
        // Arrange
        val leg = onChain(
            type = TransactionType.Swap,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val result = converter.convert(
            expressSwap(status = ExpressExchangeStatus.Finished, txInfo = leg, provider = provider(name = "Changelly")),
        )

        // Assert
        assertThat(result.rows.map { it.label }).containsExactly(
            resourceReference(R.string.express_provider),
            resourceReference(R.string.common_rate),
            resourceReference(R.string.common_network_fee_title),
        ).inOrder()
    }

    @Test
    fun `GIVEN express swap with both amounts WHEN convert THEN rate row 1 from approx to follows provider`() {
        // Act — no on-chain leg, so the rows are provider then rate.
        val result = converter.convert(
            expressSwap(status = ExpressExchangeStatus.Finished, provider = provider(name = "Changelly")),
        )

        // Assert
        assertThat(result.rows.map { it.label }).containsExactly(
            resourceReference(R.string.express_provider),
            resourceReference(R.string.common_rate),
        ).inOrder()
        val rate = result.rows[1].value.resolveString()
        // 0.001 BTC / 1.5 ETH ≈ 0.00066667; base falls back to the unresolved from-leg network id, quote to BTC.
        assertThat(rate).startsWith("1")
        assertThat(rate).contains("≈")
        assertThat(rate).contains("ethereum")
        assertThat(rate).contains("BTC")
    }

    @Test
    fun `GIVEN express swap with non-positive amount WHEN convert THEN no rate row`() {
        // Arrange — a zero pay-in makes the rate undefined; the row is dropped (division-by-zero guard).
        val base = expressSwap(status = ExpressExchangeStatus.Finished, provider = provider(name = "Changelly"))
        val swap = base.copy(tx = base.tx.copy(fromAsset = base.tx.fromAsset.copy(amount = BigDecimal.ZERO)))

        // Act
        val result = converter.convert(swap)

        // Assert — only the provider row remains.
        assertThat(result.rows.map { it.label }).containsExactly(resourceReference(R.string.express_provider))
    }

    @Test
    fun `GIVEN express onramp with both amounts WHEN convert THEN rate row 1 crypto approx fiat`() {
        // Act
        val result = converter.convert(expressOnramp(status = ExpressOnrampStatus.Finished))

        // Assert — onramp has no provider in the fixture, so the only row is the rate.
        assertThat(result.rows.map { it.label }).containsExactly(resourceReference(R.string.common_rate))
        val rate = result.rows.first().value.resolveString()
        // 100 SEK / 0.006 BTC ≈ 16,666.67 SEK; base is the resolved crypto symbol (BTC).
        assertThat(rate).startsWith("1")
        assertThat(rate).contains("≈")
        assertThat(rate).contains("BTC")
        assertThat(rate).contains("SEK")
    }

    // endregion

    // region Provider button

    @Test
    fun `GIVEN failed express swap with url WHEN convert THEN go-to-provider button opening the url`() {
        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Failed, externalTxUrl = EXTERNAL_URL))

        // Assert
        val button = result.providerButton
        assertThat(button?.text).isEqualTo(resourceReference(R.string.common_go_to_provider))
        button?.onClick?.invoke()
        assertThat(openedUrls).containsExactly(EXTERNAL_URL)
    }

    @Test
    fun `GIVEN verifying express swap with url WHEN convert THEN go-to-verification button`() {
        // Act
        val result = converter.convert(
            expressSwap(status = ExpressExchangeStatus.Verifying, externalTxUrl = EXTERNAL_URL),
        )

        // Assert
        assertThat(result.providerButton?.text).isEqualTo(resourceReference(R.string.common_go_to_verification))
    }

    @Test
    fun `GIVEN verifying express onramp with url WHEN convert THEN go-to-verification button opening the url`() {
        // Act
        val result = converter.convert(
            expressOnramp(status = ExpressOnrampStatus.Verifying, externalTxUrl = EXTERNAL_URL),
        )

        // Assert
        val button = result.providerButton
        assertThat(button?.text).isEqualTo(resourceReference(R.string.common_go_to_verification))
        button?.onClick?.invoke()
        assertThat(openedUrls).containsExactly(EXTERNAL_URL)
    }

    @Test
    fun `GIVEN failed express swap without url WHEN convert THEN no provider button`() {
        // Act — the provider supplies no link (e.g. DEX), so there is nowhere to send the user.
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Failed, externalTxUrl = null))

        // Assert
        assertThat(result.providerButton).isNull()
    }

    @Test
    fun `GIVEN finished express swap with url WHEN convert THEN no provider button`() {
        // Act — a settled success needs no provider action even when a link exists.
        val result = converter.convert(
            expressSwap(status = ExpressExchangeStatus.Finished, externalTxUrl = EXTERNAL_URL),
        )

        // Assert
        assertThat(result.providerButton).isNull()
    }

    // endregion

    // region Leg owner

    @Test
    fun `GIVEN swap between own accounts WHEN convert THEN legs labelled From-To with account owners`() {
        // Arrange — from leg on ethereum, payout leg on bitcoin, both addresses owned, accounts mode on.
        val swap = expressSwap(
            status = ExpressExchangeStatus.Finished,
            fromAddress = FROM_ADDRESS,
            payoutAddress = PAYOUT_ADDRESS,
            fromCurrency = currency,
        )
        val lookup = lookupOf(
            currency.network.id.rawId to mapOf(FROM_ADDRESS to ownAccount),
            bitcoin.network.id.rawId to mapOf(PAYOUT_ADDRESS to ownAccount),
        )

        // Act
        val result = expressConverter(lookup = lookup).convert(swap)

        // Assert
        assertThat(result.from?.label).isEqualTo(resourceReference(R.string.common_from))
        assertThat(result.to?.label).isEqualTo(resourceReference(R.string.common_to))
        assertThat(result.from?.owner).isInstanceOf(TxHistoryDetailsUM.AssetOwnerUM.Account::class.java)
        assertThat(result.to?.owner).isInstanceOf(TxHistoryDetailsUM.AssetOwnerUM.Account::class.java)
    }

    @Test
    fun `GIVEN swap to own address with accounts mode off WHEN convert THEN owner is wallet`() {
        // Arrange
        val swap = expressSwap(status = ExpressExchangeStatus.Finished, payoutAddress = PAYOUT_ADDRESS)
        val lookup = lookupOf(
            bitcoin.network.id.rawId to mapOf(PAYOUT_ADDRESS to ownAccount),
            isAccountsModeEnabled = false,
        )

        // Act
        val result = expressConverter(lookup = lookup).convert(swap)

        // Assert
        val owner = requireNotNull(result.to?.owner)
        assertThat(owner).isInstanceOf(TxHistoryDetailsUM.AssetOwnerUM.Wallet::class.java)
        assertThat((owner as TxHistoryDetailsUM.AssetOwnerUM.Wallet).name).isEqualTo(stringReference("My Wallet"))
    }

    @Test
    fun `GIVEN send-and-swap to external address WHEN convert THEN to leg owner is external address`() {
        // Arrange — payout address is none of the user's, so it stays an external address.
        val swap = expressSwap(status = ExpressExchangeStatus.Finished, payoutAddress = EXTERNAL_ADDRESS)

        // Act
        val result = expressConverter(lookup = lookupOf()).convert(swap)

        // Assert
        val owner = requireNotNull(result.to?.owner)
        assertThat(owner).isInstanceOf(TxHistoryDetailsUM.AssetOwnerUM.Address::class.java)
        assertThat((owner as TxHistoryDetailsUM.AssetOwnerUM.Address).rawAddress).isEqualTo(EXTERNAL_ADDRESS)
        assertThat(result.to?.label).isEqualTo(resourceReference(R.string.common_to))
    }

    @Test
    fun `GIVEN onramp to own account WHEN convert THEN from is You paid and to has account owner`() {
        // Arrange
        val onramp = expressOnramp(status = ExpressOnrampStatus.Finished, payoutAddress = PAYOUT_ADDRESS)
        val lookup = lookupOf(bitcoin.network.id.rawId to mapOf(PAYOUT_ADDRESS to ownAccount))

        // Act
        val result = expressConverter(lookup = lookup).convert(onramp)

        // Assert
        assertThat(result.from?.owner).isNull()
        assertThat(result.from?.label).isEqualTo(resourceReference(R.string.tx_history_you_paid))
        assertThat(result.to?.owner).isInstanceOf(TxHistoryDetailsUM.AssetOwnerUM.Account::class.java)
        assertThat(result.to?.label).isEqualTo(resourceReference(R.string.common_to))
    }

    @Test
    fun `GIVEN leg with unresolved currency but own address WHEN convert THEN owner resolved cross-network`() {
        // Arrange — from leg has no cryptoCurrency (null network), yet its address is owned on exactly one network.
        val swap = expressSwap(
            status = ExpressExchangeStatus.Finished,
            fromAddress = FROM_ADDRESS,
            fromCurrency = null,
        )
        val lookup = lookupOf(currency.network.id.rawId to mapOf(FROM_ADDRESS to ownAccount))

        // Act
        val result = expressConverter(lookup = lookup).convert(swap)

        // Assert
        assertThat(result.from?.owner).isInstanceOf(TxHistoryDetailsUM.AssetOwnerUM.Account::class.java)
        assertThat(result.from?.label).isEqualTo(resourceReference(R.string.common_from))
    }

    @Test
    fun `GIVEN leg with unresolved currency and address on two distinct accounts WHEN convert THEN stays external`() {
        // Arrange — null network forces a cross-network lookup; the same address maps to two different accounts.
        val swap = expressSwap(
            status = ExpressExchangeStatus.Finished,
            fromAddress = FROM_ADDRESS,
            fromCurrency = null,
        )
        val lookup = lookupOf(
            currency.network.id.rawId to mapOf(FROM_ADDRESS to ownAccount),
            bitcoin.network.id.rawId to mapOf(FROM_ADDRESS to secondAccount),
        )

        // Act
        val result = expressConverter(lookup = lookup).convert(swap)

        // Assert — ambiguous, so it falls back to the external address rather than guessing an owner.
        val owner = requireNotNull(result.from?.owner)
        assertThat(owner).isInstanceOf(TxHistoryDetailsUM.AssetOwnerUM.Address::class.java)
        assertThat((owner as TxHistoryDetailsUM.AssetOwnerUM.Address).rawAddress).isEqualTo(FROM_ADDRESS)
    }

    // endregion
}