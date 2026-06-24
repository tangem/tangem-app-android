package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.express.models.OnrampTransaction
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExpressTxToTransactionItemUMConverterTest {

    private val txHistoryUiActions: TxHistoryUiActions = mockk(relaxed = true)
    private val coin: CryptoCurrency.Coin = createCoin(symbol = "ETH", decimals = 18)

    private val converter = ExpressTxToTransactionItemUMConverter(
        currency = coin,
        txHistoryUiActions = txHistoryUiActions,
    )

    // region Status → bucket

    @Test
    fun `GIVEN every swap status WHEN convert THEN mapped to expected status bucket`() {
        val cases = mapOf(
            ExpressExchangeStatus.Finished to Status.Confirmed,
            ExpressExchangeStatus.Failed to Status.Failed,
            ExpressExchangeStatus.TxFailed to Status.Failed,
            ExpressExchangeStatus.Refunded to Status.Failed,
            ExpressExchangeStatus.Expired to Status.Failed,
            ExpressExchangeStatus.Unknown to Status.Failed,
            ExpressExchangeStatus.Preview to Status.Unconfirmed,
            ExpressExchangeStatus.Created to Status.Unconfirmed,
            ExpressExchangeStatus.ExchangeTxSent to Status.Unconfirmed,
            ExpressExchangeStatus.Waiting to Status.Unconfirmed,
            ExpressExchangeStatus.WaitingTxHash to Status.Unconfirmed,
            ExpressExchangeStatus.Confirming to Status.Unconfirmed,
            ExpressExchangeStatus.Exchanging to Status.Unconfirmed,
            ExpressExchangeStatus.Sending to Status.Unconfirmed,
            ExpressExchangeStatus.Verifying to Status.Unconfirmed,
            ExpressExchangeStatus.Paused to Status.Unconfirmed,
        )
        // every enum entry is covered (guards against new statuses silently falling through)
        assertThat(cases.keys).containsExactlyElementsIn(ExpressExchangeStatus.entries)

        cases.forEach { (status, expected) ->
            val result = converter.convert(createSwap(status = status)) as TransactionItemUM.Content
            assertWithMessage(status.name).that(result.status).isEqualTo(expected)
        }
    }

    @Test
    fun `GIVEN every onramp status WHEN convert THEN mapped to expected status bucket`() {
        val cases = mapOf(
            ExpressOnrampStatus.Finished to Status.Confirmed,
            ExpressOnrampStatus.Failed to Status.Failed,
            ExpressOnrampStatus.Expired to Status.Failed,
            ExpressOnrampStatus.Unknown to Status.Failed,
            ExpressOnrampStatus.Created to Status.Unconfirmed,
            ExpressOnrampStatus.WaitingForPayment to Status.Unconfirmed,
            ExpressOnrampStatus.PaymentProcessing to Status.Unconfirmed,
            ExpressOnrampStatus.Verifying to Status.Unconfirmed,
            ExpressOnrampStatus.Paid to Status.Unconfirmed,
            ExpressOnrampStatus.Sending to Status.Unconfirmed,
            ExpressOnrampStatus.Paused to Status.Unconfirmed,
        )
        assertThat(cases.keys).containsExactlyElementsIn(ExpressOnrampStatus.entries)

        cases.forEach { (status, expected) ->
            val result = converter.convert(createOnramp(status = status)) as TransactionItemUM.Content
            assertWithMessage(status.name).that(result.status).isEqualTo(expected)
        }
    }

    // endregion

    // region Amount sign / prefix

    @Test
    fun `GIVEN outgoing swap WHEN convert THEN amount is negative from-leg`() {
        val result = converter.convert(
            createSwap(status = ExpressExchangeStatus.Waiting, isOutgoing = true),
        ) as TransactionItemUM.Content

        assertThat(result.direction).isEqualTo(TransactionItemUM.Content.Direction.OUTGOING)
        assertThat(result.amount).startsWith("-")
        assertThat(result.amount).contains("1.5")
    }

    @Test
    fun `GIVEN incoming swap WHEN convert THEN amount is positive to-leg`() {
        val result = converter.convert(
            createSwap(status = ExpressExchangeStatus.Waiting, isOutgoing = false),
        ) as TransactionItemUM.Content

        assertThat(result.direction).isEqualTo(TransactionItemUM.Content.Direction.INCOMING)
        assertThat(result.amount).startsWith("+")
        assertThat(result.amount).contains("0.001")
    }

    @Test
    fun `GIVEN finished onramp WHEN convert THEN amount prefixed with plus`() {
        val result = converter.convert(createOnramp(status = ExpressOnrampStatus.Finished)) as TransactionItemUM.Content
        assertThat(result.amount).startsWith("+")
    }

    @Test
    fun `GIVEN in-progress onramp WHEN convert THEN amount prefixed with tilde`() {
        val result = converter.convert(createOnramp(status = ExpressOnrampStatus.Sending)) as TransactionItemUM.Content
        assertThat(result.amount).startsWith("~")
    }

    @Test
    fun `GIVEN failed onramp WHEN convert THEN amount has no sign prefix`() {
        val result = converter.convert(createOnramp(status = ExpressOnrampStatus.Failed)) as TransactionItemUM.Content
        assertThat(requireNotNull(result.amount).first())
            .isIn(listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'))
    }

    @Test
    fun `GIVEN swap with null viewed amount WHEN convert THEN amount is null`() {
        val result = converter.convert(
            createSwap(status = ExpressExchangeStatus.Waiting, isOutgoing = true, fromAmount = null),
        ) as TransactionItemUM.Content

        assertThat(result.amount).isNull()
    }

    @Test
    fun `GIVEN onramp with null amount WHEN convert THEN amount is null`() {
        val result = converter.convert(
            createOnramp(status = ExpressOnrampStatus.Sending, toAmount = null),
        ) as TransactionItemUM.Content

        assertThat(result.amount).isNull()
    }

    // endregion

    // region Title / subtitle / warning / click

    @Test
    fun `GIVEN swap statuses WHEN convert THEN status-aware title`() {
        val swapping = converter.convert(createSwap(status = ExpressExchangeStatus.Waiting)) as TransactionItemUM.Content
        val swapped = converter.convert(createSwap(status = ExpressExchangeStatus.Finished)) as TransactionItemUM.Content

        assertThat(swapping.title).isEqualTo(resourceReference(R.string.common_swapping))
        assertThat(swapped.title).isEqualTo(resourceReference(R.string.common_swapped))
    }

    @Test
    fun `GIVEN onramp statuses WHEN convert THEN status-aware title`() {
        val topUp = converter.convert(createOnramp(status = ExpressOnrampStatus.Sending)) as TransactionItemUM.Content
        val toppedUp = converter.convert(createOnramp(status = ExpressOnrampStatus.Finished)) as TransactionItemUM.Content

        assertThat(topUp.title).isEqualTo(resourceReference(R.string.tx_history_onramp_top_up))
        assertThat(toppedUp.title).isEqualTo(resourceReference(R.string.tx_history_onramp_topped_up))
    }

    @Test
    fun `GIVEN outgoing swap WHEN convert THEN subtitle shows TO counterparty ticker`() {
        val result = converter.convert(
            createSwap(status = ExpressExchangeStatus.Waiting, isOutgoing = true),
        ) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.Asset
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.TO)
        assertThat(subtitle.symbol).isEqualTo("btc") // mock: counterparty (to-leg) networkId
    }

    @Test
    fun `GIVEN onramp WHEN convert THEN subtitle shows FROM fiat code`() {
        val result = converter.convert(createOnramp(status = ExpressOnrampStatus.Sending)) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.Asset
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.FROM)
        assertThat(subtitle.symbol).isEqualTo("SEK")
    }

    @Test
    fun `GIVEN matched on-chain leg WHEN row clicked THEN opens explorer by match hash`() {
        val result = converter.convert(
            createSwap(status = ExpressExchangeStatus.Waiting, matchHash = "0xhash", isOutgoing = true),
        ) as TransactionItemUM.Content

        result.onClick()

        verify { txHistoryUiActions.openTxInExplorer("0xhash") }
    }

    // endregion

    private fun createSwap(
        status: ExpressExchangeStatus,
        matchHash: String? = null,
        isOutgoing: Boolean = true,
        fromAmount: BigDecimal? = BigDecimal("1.5"),
        toAmount: BigDecimal? = BigDecimal("0.001"),
    ) = ExpressTx.Swap(
        tx = ExchangeTransaction(
            txId = "tx-1",
            status = status,
            createdAtMillis = 100,
            provider = null,
            payinHash = matchHash.takeIf { isOutgoing },
            payoutHash = matchHash.takeUnless { isOutgoing },
            fromAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "eth", contractAddress = "0"),
                amount = fromAmount,
                decimals = 18,
            ),
            toAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "btc", contractAddress = "0xt"),
                amount = toAmount,
                decimals = 8,
            ),
        ),
        isOutgoing = isOutgoing,
        txInfo = null,
    )

    private fun createOnramp(
        status: ExpressOnrampStatus,
        toAmount: BigDecimal? = BigDecimal("0.006339"),
    ) = ExpressTx.Onramp(
        tx = OnrampTransaction(
            txId = "tx-2",
            status = status,
            createdAtMillis = 100,
            provider = null,
            payoutHash = null,
            fromFiat = Amount(
                currencySymbol = "SEK",
                value = BigDecimal("100"),
                decimals = 2,
                type = AmountType.FiatType(code = "SEK"),
            ),
            toAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "btc", contractAddress = "0"),
                amount = toAmount,
                decimals = 8,
            ),
        ),
        txInfo = null,
    )

    private fun createCoin(symbol: String, decimals: Int): CryptoCurrency.Coin = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "ethereum"),
        ),
        network = Network(
            id = Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None),
            name = "Ethereum",
            currencySymbol = symbol,
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        ),
        name = "Ethereum",
        symbol = symbol,
        decimals = decimals,
        iconUrl = null,
        isCustom = false,
    )
}