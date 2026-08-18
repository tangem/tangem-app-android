package com.tangem.features.txhistory.converter

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.express.models.OnrampTransaction
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.WalletInfo
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.test.mock.MockAccounts
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExpressTxToTransactionItemUMConverterTest {

    private val txHistoryUiActions: TxHistoryUiActions = mockk(relaxed = true)
    private val coin: CryptoCurrency.Coin = createCoin(symbol = "ETH", decimals = 18)

    private val mockCurrencyFactory = MockCryptoCurrencyFactory()
    private val ethereum = mockCurrencyFactory.ethereum
    private val bitcoin = mockCurrencyFactory.bitcoin
    private val ownAccount: Account.CryptoPortfolio = MockAccounts.createAccount(derivationIndex = 1, name = "Family")
    private val secondAccount: Account.CryptoPortfolio =
        MockAccounts.createAccount(derivationIndex = 2, name = "Savings")

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
            ExpressOnrampStatus.Refunded to Status.Failed,
            ExpressOnrampStatus.Unknown to Status.Failed,
            ExpressOnrampStatus.Created to Status.Unconfirmed,
            ExpressOnrampStatus.WaitingForPayment to Status.Unconfirmed,
            ExpressOnrampStatus.PaymentProcessing to Status.Unconfirmed,
            ExpressOnrampStatus.Verifying to Status.Unconfirmed,
            ExpressOnrampStatus.Paid to Status.Unconfirmed,
            ExpressOnrampStatus.Sending to Status.Unconfirmed,
            ExpressOnrampStatus.Paused to Status.Unconfirmed,
            ExpressOnrampStatus.RefundInProgress to Status.Unconfirmed,
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
        val swapping = converter.convert(
            createSwap(status = ExpressExchangeStatus.Waiting),
        ) as TransactionItemUM.Content
        val swapped = converter.convert(
            createSwap(status = ExpressExchangeStatus.Finished),
        ) as TransactionItemUM.Content

        assertThat(swapping.title).isEqualTo(resourceReference(R.string.common_swapping))
        assertThat(swapped.title).isEqualTo(resourceReference(R.string.common_swapped))
    }

    @Test
    fun `GIVEN onramp statuses WHEN convert THEN status-aware title`() {
        val topUp = converter.convert(createOnramp(status = ExpressOnrampStatus.Sending)) as TransactionItemUM.Content
        val toppedUp = converter.convert(
            createOnramp(status = ExpressOnrampStatus.Finished),
        ) as TransactionItemUM.Content

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
    fun `GIVEN express row WHEN row clicked THEN opens in-app details for that tx`() {
        val swap = createSwap(status = ExpressExchangeStatus.Waiting, isOutgoing = true)
        val result = converter.convert(swap) as TransactionItemUM.Content

        result.onClick()

        verify { txHistoryUiActions.onTransactionClick(swap) }
    }

    // endregion

    // region Owner tail

    @Test
    fun `GIVEN no lookup WHEN convert THEN subtitle has no owner tail`() {
        val result = converter.convert(
            createSwap(status = ExpressExchangeStatus.Finished, fromCurrency = ethereum, toCurrency = bitcoin),
        ) as TransactionItemUM.Content

        assertThat((result.subtitle as ContentSubtitle.Asset).owner).isNull()
    }

    @Test
    fun `GIVEN swap within one own account WHEN convert THEN subtitle has no owner tail`() {
        // Arrange — from and payout legs both in the same own account: nothing to disambiguate.
        val swap = createSwap(
            status = ExpressExchangeStatus.Finished,
            fromCurrency = ethereum,
            toCurrency = bitcoin,
            fromAddress = FROM_ADDRESS,
            payoutAddress = PAYOUT_ADDRESS,
        )
        val lookup = lookupOf(
            ethereum.network.id.rawId to mapOf(FROM_ADDRESS to ownAccount),
            bitcoin.network.id.rawId to mapOf(PAYOUT_ADDRESS to ownAccount),
        )

        // Act
        val result = converterWith(lookup).convert(swap) as TransactionItemUM.Content

        // Assert
        assertThat((result.subtitle as ContentSubtitle.Asset).owner).isNull()
    }

    @Test
    fun `GIVEN outgoing swap to a different own account WHEN convert THEN subtitle names the payout account`() {
        // Arrange — payout leg owned by a different account than the pay-in leg, accounts mode on.
        val swap = createSwap(
            status = ExpressExchangeStatus.Finished,
            isOutgoing = true,
            fromCurrency = ethereum,
            toCurrency = bitcoin,
            fromAddress = FROM_ADDRESS,
            payoutAddress = PAYOUT_ADDRESS,
        )
        val lookup = lookupOf(
            ethereum.network.id.rawId to mapOf(FROM_ADDRESS to ownAccount),
            bitcoin.network.id.rawId to mapOf(PAYOUT_ADDRESS to secondAccount),
        )

        // Act — outgoing row shows the TO (payout) counterparty.
        val result = converterWith(lookup).convert(swap) as TransactionItemUM.Content

        // Assert
        val owner = (result.subtitle as ContentSubtitle.Asset).owner
        assertThat(owner).isInstanceOf(ContentSubtitle.AssetOwner.Account::class.java)
        assertThat((owner as ContentSubtitle.AssetOwner.Account).name)
            .isEqualTo(secondAccount.accountName.toUM().value)
    }

    @Test
    fun `GIVEN incoming swap from a different own account WHEN convert THEN subtitle names the source account`() {
        // Arrange — incoming row (viewed token is the payout side) shows the FROM (pay-in) counterparty account.
        val swap = createSwap(
            status = ExpressExchangeStatus.Finished,
            isOutgoing = false,
            fromCurrency = ethereum,
            toCurrency = bitcoin,
            fromAddress = FROM_ADDRESS,
            payoutAddress = PAYOUT_ADDRESS,
        )
        val lookup = lookupOf(
            ethereum.network.id.rawId to mapOf(FROM_ADDRESS to secondAccount),
            bitcoin.network.id.rawId to mapOf(PAYOUT_ADDRESS to ownAccount),
        )

        // Act
        val result = converterWith(lookup).convert(swap) as TransactionItemUM.Content

        // Assert
        val owner = (result.subtitle as ContentSubtitle.Asset).owner
        assertThat(owner).isInstanceOf(ContentSubtitle.AssetOwner.Account::class.java)
        assertThat((owner as ContentSubtitle.AssetOwner.Account).name)
            .isEqualTo(secondAccount.accountName.toUM().value)
    }

    @Test
    fun `GIVEN accounts mode off with multiple wallets WHEN convert THEN subtitle names the payout wallet`() {
        // Arrange — payout resolves to an own wallet; more than one wallet, so it is worth naming.
        val swap = createSwap(
            status = ExpressExchangeStatus.Finished,
            isOutgoing = true,
            toCurrency = bitcoin,
            payoutAddress = PAYOUT_ADDRESS,
        )
        val lookup = lookupOf(
            bitcoin.network.id.rawId to mapOf(PAYOUT_ADDRESS to ownAccount),
            isAccountsModeEnabled = false,
            walletInfoById = twoWalletInfo(),
        )

        // Act
        val result = converterWith(lookup).convert(swap) as TransactionItemUM.Content

        // Assert
        val owner = (result.subtitle as ContentSubtitle.Asset).owner
        assertThat(owner).isInstanceOf(ContentSubtitle.AssetOwner.Wallet::class.java)
        assertThat((owner as ContentSubtitle.AssetOwner.Wallet).name).isEqualTo("My Wallet")
    }

    @Test
    fun `GIVEN accounts mode off with a single wallet WHEN convert THEN subtitle has no owner tail`() {
        // Arrange — an own-wallet leg with a single wallet has nothing to disambiguate.
        val swap = createSwap(
            status = ExpressExchangeStatus.Finished,
            isOutgoing = true,
            toCurrency = bitcoin,
            payoutAddress = PAYOUT_ADDRESS,
        )
        val lookup = lookupOf(
            bitcoin.network.id.rawId to mapOf(PAYOUT_ADDRESS to ownAccount),
            isAccountsModeEnabled = false,
        )

        // Act
        val result = converterWith(lookup).convert(swap) as TransactionItemUM.Content

        // Assert
        assertThat((result.subtitle as ContentSubtitle.Asset).owner).isNull()
    }

    @Test
    fun `GIVEN send-and-swap to an external address WHEN convert THEN subtitle has no owner tail`() {
        // Arrange — payout goes to a non-owned address; the external counterparty adds no "in …" tail.
        val swap = createSwap(
            status = ExpressExchangeStatus.Finished,
            isOutgoing = true,
            fromCurrency = ethereum,
            toCurrency = bitcoin,
            fromAddress = FROM_ADDRESS,
            payoutAddress = "external-address",
        )
        val lookup = lookupOf(ethereum.network.id.rawId to mapOf(FROM_ADDRESS to ownAccount))

        // Act
        val result = converterWith(lookup).convert(swap) as TransactionItemUM.Content

        // Assert
        assertThat((result.subtitle as ContentSubtitle.Asset).owner).isNull()
    }

    // endregion

    @Suppress("LongParameterList")
    private fun createSwap(
        status: ExpressExchangeStatus,
        isOutgoing: Boolean = true,
        fromAmount: BigDecimal? = BigDecimal("1.5"),
        toAmount: BigDecimal? = BigDecimal("0.001"),
        fromAddress: String = "from-addr",
        payoutAddress: String = "payout-addr",
        fromCurrency: CryptoCurrency? = null,
        toCurrency: CryptoCurrency? = null,
    ) = ExpressTx.Swap(
        tx = ExchangeTransaction(
            txId = "tx-1",
            status = status,
            createdAtMillis = 100,
            provider = null,
            payinHash = null,
            payoutHash = null,
            fromAddress = fromAddress,
            payoutAddress = payoutAddress,
            fromAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "eth", contractAddress = "0"),
                amount = fromAmount,
                decimals = 18,
                cryptoCurrency = fromCurrency,
            ),
            toAsset = ExpressTransactionAsset(
                id = ExpressAssetId(networkId = "btc", contractAddress = "0xt"),
                amount = toAmount,
                decimals = 8,
                cryptoCurrency = toCurrency,
            ),
            externalTxUrl = null,
            externalTxId = null,
            payinAddress = "payin-addr",
            updatedAtMillis = 100,
            refundAssetId = null,
            refundCurrency = null,
            fromAmount = fromAmount ?: BigDecimal.ZERO,
            toAmount = toAmount ?: BigDecimal.ZERO,
            toActualAmount = null,
        ),
        isOutgoing = isOutgoing,
        txInfo = null,
    )

    private fun converterWith(lookup: TxHistoryLookupContext) = ExpressTxToTransactionItemUMConverter(
        currency = coin,
        txHistoryUiActions = txHistoryUiActions,
        lookup = lookup,
    )

    private fun lookupOf(
        vararg networks: Pair<Network.RawID, Map<String, Account>>,
        isAccountsModeEnabled: Boolean = true,
        walletInfoById: Map<UserWalletId, WalletInfo> = singleWalletInfo(),
    ): TxHistoryLookupContext = TxHistoryLookupContext(
        ownAccountByNetwork = networks.toMap(),
        isAccountsModeEnabled = isAccountsModeEnabled,
        walletInfoById = walletInfoById,
    )

    private fun singleWalletInfo(): Map<UserWalletId, WalletInfo> = mapOf(
        MockAccounts.userWalletId to WalletInfo(name = "My Wallet", deviceIconUM = deviceIcon()),
    )

    private fun twoWalletInfo(): Map<UserWalletId, WalletInfo> = mapOf(
        MockAccounts.userWalletId to WalletInfo(name = "My Wallet", deviceIconUM = deviceIcon()),
        UserWalletId("022") to WalletInfo(name = "Second Wallet", deviceIconUM = deviceIcon()),
    )

    private fun deviceIcon(): DeviceIconUM = DeviceIconUM.Card(mainColor = Color(0xFF1E1E1E), secondColor = null)

    private companion object {
        const val FROM_ADDRESS = "0xfromOwnAddress1234"
        const val PAYOUT_ADDRESS = "bc1qPayoutOwnAddress"
    }

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
            payoutAddress = "payout-addr",
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
            externalTxUrl = null,
            fiatCurrency = null,
            toAmount = toAmount,
            toActualAmount = null,
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