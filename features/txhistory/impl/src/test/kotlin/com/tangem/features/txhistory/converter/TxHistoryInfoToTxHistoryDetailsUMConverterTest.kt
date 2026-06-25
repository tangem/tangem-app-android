package com.tangem.features.txhistory.converter

import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.res.generated.icons.ic_globe_24
import com.tangem.core.ui.res.generated.icons.ic_share_android_24
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset.ID as ExpressAssetId
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.express.models.OnrampTransaction
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.WalletInfo
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryInfoToTxHistoryDetailsUMConverterTest {

    private val mockCurrencyFactory = MockCryptoCurrencyFactory()
    private val currency = mockCurrencyFactory.ethereum

    // The express payout leg: a real Bitcoin coin so the resolved symbol (BTC) matches the "bitcoin" network id.
    private val bitcoin = mockCurrencyFactory.bitcoin
    private val ownAccount: Account.CryptoPortfolio = MockAccounts.createAccount(derivationIndex = 1, name = "Family")
    private val secondAccount: Account.CryptoPortfolio = MockAccounts.createAccount(derivationIndex = 2, name = "Savings")
    private val copiedAddresses = mutableListOf<String>()
    private val openedUrls = mutableListOf<String>()
    private val converter = TxHistoryInfoToTxHistoryDetailsUMConverter(
        currency = currency,
        onCopyAddress = copiedAddresses::add,
        onGoToProvider = openedUrls::add,
    )

    @BeforeEach
    fun setUp() {
        copiedAddresses.clear()
        openedUrls.clear()
        // The header subtitle formats the date via DateTimeFormatters -> DateFormat.getBestDateTimePattern,
        // which is an Android stub on the JVM. Mirror the DateTimeFormattersTest mock so convert() runs.
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
    }

    // region On-chain (TxInfo)

    @Test
    fun `GIVEN on-chain Swap WHEN convert THEN SingleAsset fallback`() {
        // A two-asset swap always surfaces as ExpressTx.Swap; an on-chain TxInfo of type Swap has no legs,
        // so it falls back to the single amount it does carry rather than an empty two-asset card.
        // Arrange
        val tx = onChain(type = TransactionType.Swap)

        // Act
        val result = converter.convert(tx)

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.SingleAsset::class.java)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN non-Swap TransactionType WHEN convert THEN SingleAsset`(type: TransactionType) {
        // Act
        val result = converter.convert(onChain(type = type))

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.SingleAsset::class.java)
    }

    private fun provideTestModels() = listOf(
        TransactionType.Transfer,
        TransactionType.Approve,
        TransactionType.Operation(name = "Mint NFT"),
        TransactionType.UnknownOperation,
        TransactionType.GaslessFee,
        TransactionType.Staking.Stake,
        TransactionType.Staking.ClaimRewards,
        TransactionType.Staking.Vote(validatorAddress = VALIDATOR_ADDRESS),
        TransactionType.YieldSupply.Topup,
        TransactionType.YieldSupply.Enter(address = USER_ADDRESS),
    )

    @Test
    fun `GIVEN incoming confirmed external Transfer WHEN convert THEN header has down icon, confirmed status, received title`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val header = converter.convert(tx).header

        // Assert
        assertThat(header.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_down_20))
        assertThat(header.status).isEqualTo(TransactionItemUM.Content.Status.Confirmed)
        assertThat(header.title).isEqualTo(resourceReference(R.string.common_received))
    }

    @Test
    fun `GIVEN outgoing external Transfer WHEN convert THEN sent title`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val header = converter.convert(tx).header

        // Assert
        assertThat(header.title).isEqualTo(resourceReference(R.string.common_sent))
    }

    @Test
    fun `GIVEN incoming Transfer from own address WHEN convert THEN transferred title`() {
        // Arrange — the counterparty is one of the user's own deposit addresses.
        val ownConverter = TxHistoryInfoToTxHistoryDetailsUMConverter(
            currency = currency,
            onCopyAddress = copiedAddresses::add,
            onGoToProvider = openedUrls::add,
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = onChain(
            type = TransactionType.Transfer,
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val header = ownConverter.convert(tx).header

        // Assert
        assertThat(header.title).isEqualTo(resourceReference(R.string.common_transferred))
    }

    @Test
    fun `GIVEN outgoing Transfer to own address WHEN convert THEN transferred title`() {
        // Arrange
        val ownConverter = TxHistoryInfoToTxHistoryDetailsUMConverter(
            currency = currency,
            onCopyAddress = copiedAddresses::add,
            onGoToProvider = openedUrls::add,
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = onChain(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val header = ownConverter.convert(tx).header

        // Assert
        assertThat(header.title).isEqualTo(resourceReference(R.string.common_transferred))
    }

    @Test
    fun `GIVEN Swap WHEN convert THEN header has exchange icon`() {
        // Arrange
        val tx = onChain(type = TransactionType.Swap)

        // Act
        val header = converter.convert(tx).header

        // Assert
        assertThat(header.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20))
    }

    @Test
    fun `GIVEN menu callbacks WHEN convert THEN header menu has copy-id, share and explore rows wired`() {
        // Arrange
        var copiedTxId = false
        var shared = false
        var explored = false
        val menuConverter = TxHistoryInfoToTxHistoryDetailsUMConverter(
            currency = currency,
            onCopyAddress = copiedAddresses::add,
            onGoToProvider = openedUrls::add,
            onCopyTxId = { copiedTxId = true },
            onShare = { shared = true },
            onExplore = { explored = true },
        )

        // Act
        val menu = menuConverter.convert(onChain(type = TransactionType.Transfer)).header.menu

        // Assert
        assertThat(menu).hasSize(3)
        assertThat(menu[0].icon).isEqualTo(Icons.ic_copy_24)
        assertThat(menu[0].title).isEqualTo(resourceReference(R.string.common_transaction_id))
        assertThat(menu[1].icon).isEqualTo(Icons.ic_share_android_24)
        assertThat(menu[1].title).isEqualTo(resourceReference(R.string.common_share))
        assertThat(menu[2].icon).isEqualTo(Icons.ic_globe_24)
        assertThat(menu[2].title).isEqualTo(resourceReference(R.string.common_explore))

        menu[0].onClick()
        menu[1].onClick()
        menu[2].onClick()
        assertThat(copiedTxId).isTrue()
        assertThat(shared).isTrue()
        assertThat(explored).isTrue()
    }

    @Test
    fun `GIVEN no share and explore callbacks WHEN convert THEN header menu drops the share and explore rows`() {
        // Arrange — onShare/onExplore are null (e.g. an express op with no on-chain leg to share or open yet).
        val menuConverter = TxHistoryInfoToTxHistoryDetailsUMConverter(
            currency = currency,
            onCopyAddress = copiedAddresses::add,
            onGoToProvider = openedUrls::add,
            onCopyTxId = {},
            onShare = null,
            onExplore = null,
        )

        // Act
        val menu = menuConverter.convert(onChain(type = TransactionType.Transfer)).header.menu

        // Assert
        assertThat(menu).hasSize(1)
        assertThat(menu[0].title).isEqualTo(resourceReference(R.string.common_transaction_id))
    }

    @Test
    fun `GIVEN no menu callbacks WHEN convert THEN header menu is empty`() {
        // Arrange — every menu action is absent (e.g. a blank tx id with no on-chain leg to share or open).
        val menuConverter = TxHistoryInfoToTxHistoryDetailsUMConverter(
            currency = currency,
            onCopyAddress = copiedAddresses::add,
            onGoToProvider = openedUrls::add,
            onCopyTxId = null,
            onShare = null,
            onExplore = null,
        )

        // Act
        val menu = menuConverter.convert(onChain(type = TransactionType.Transfer)).header.menu

        // Assert
        assertThat(menu).isEmpty()
    }

    @Test
    fun `GIVEN incoming Transfer WHEN convert THEN amount block has plus sign and not failed`() {
        // Arrange
        val tx = onChain(type = TransactionType.Transfer, isOutgoing = false)

        // Act
        val amountBlock = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).amountBlock

        // Assert
        assertThat(amountBlock.amount.resolveString()).startsWith("+ ")
        assertThat(amountBlock.isFailed).isFalse()
    }

    @Test
    fun `GIVEN outgoing Transfer WHEN convert THEN amount block has minus sign`() {
        // Arrange
        val tx = onChain(type = TransactionType.Transfer, isOutgoing = true)

        // Act
        val amountBlock = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).amountBlock

        // Assert
        assertThat(amountBlock.amount.resolveString()).startsWith("- ")
    }

    @Test
    fun `GIVEN zero amount WHEN convert THEN amount block has no sign`() {
        // Arrange
        val tx = onChain(type = TransactionType.Transfer, isOutgoing = true, amount = BigDecimal.ZERO)

        // Act
        val amountBlock = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).amountBlock

        // Assert
        val amount = amountBlock.amount.resolveString()
        assertThat(amount).doesNotContain("+")
        assertThat(amount).doesNotContain("-")
    }

    @Test
    fun `GIVEN failed outgoing Transfer WHEN convert THEN amount block is failed and drops the sign`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            isOutgoing = true,
            status = TxInfo.TransactionStatus.Failed,
        )

        // Act
        val amountBlock = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).amountBlock

        // Assert
        assertThat(amountBlock.isFailed).isTrue()
        val amount = amountBlock.amount.resolveString()
        assertThat(amount).doesNotContain("+")
        assertThat(amount).doesNotContain("-")
    }

    @Test
    fun `GIVEN no interaction address WHEN convert THEN counterparty is null`() {
        // Arrange
        val tx = onChain(type = TransactionType.Transfer, interactionAddressType = null)

        // Act
        val counterparty = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).counterparty

        // Assert
        assertThat(counterparty).isNull()
    }

    @Test
    fun `GIVEN incoming Transfer with User address WHEN convert THEN address-avatar counterparty with From label`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).counterparty

        // Assert
        assertThat(counterparty?.avatar).isEqualTo(TxHistoryDetailsUM.CounterpartyAvatar.Address(USER_ADDRESS))
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.common_from))
    }

    @Test
    fun `GIVEN outgoing Transfer with User address WHEN convert THEN counterparty has Recipient label`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).counterparty

        // Assert
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.send_recipient))
    }

    @Test
    fun `GIVEN address counterparty WHEN onCopyClick invoked THEN raw address is copied`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )
        val counterparty = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).counterparty

        // Act
        counterparty?.onCopyClick?.invoke()

        // Assert
        assertThat(copiedAddresses).containsExactly(USER_ADDRESS)
    }

    @Test
    fun `GIVEN tx with fee WHEN convert THEN single network-fee row`() {
        // Arrange
        val tx = onChain(
            type = TransactionType.Transfer,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val rows = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).rows

        // Assert
        assertThat(rows).hasSize(1)
        assertThat(rows.first().label).isEqualTo(resourceReference(R.string.common_network_fee_title))
        assertThat(rows.first().value.resolveString()).contains("ETH")
    }

    @Test
    fun `GIVEN tx without fee WHEN convert THEN no rows`() {
        // Arrange
        val tx = onChain(type = TransactionType.Transfer, fee = null)

        // Act
        val rows = (converter.convert(tx) as TxHistoryDetailsUM.SingleAsset).rows

        // Assert
        assertThat(rows).isEmpty()
    }

    // endregion

    // region Express (swap / onramp)

    @Test
    fun `GIVEN express swap WHEN convert THEN TwoAssets with exchange icon`() {
        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Exchanging))

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.TwoAssets::class.java)
        assertThat(result.header.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20))
    }

    @Test
    fun `GIVEN exchanging express swap WHEN convert THEN info status banner with loader`() {
        // Act
        val swap = converter.convert(expressSwap(status = ExpressExchangeStatus.Exchanging))
        val banner = (swap as TxHistoryDetailsUM.TwoAssets).statusBanner

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
        val swap = converter.convert(expressSwap(status = ExpressExchangeStatus.Verifying))
        val banner = (swap as TxHistoryDetailsUM.TwoAssets).statusBanner

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
        val swap = converter.convert(expressSwap(status = ExpressExchangeStatus.Finished))
        val banner = (swap as TxHistoryDetailsUM.TwoAssets).statusBanner

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
        // Act
        val swap = converter.convert(expressSwap(status = ExpressExchangeStatus.Unknown))

        // Assert — nothing to surface, the plaque is hidden.
        assertThat((swap as TxHistoryDetailsUM.TwoAssets).statusBanner).isNull()
    }

    @Test
    fun `GIVEN failed express swap WHEN convert THEN error status banner with refund subtitle`() {
        // Act
        val swap = converter.convert(expressSwap(status = ExpressExchangeStatus.Failed))
        val banner = (swap as TxHistoryDetailsUM.TwoAssets).statusBanner

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
    fun `GIVEN in-progress express swap WHEN convert THEN from is minus and to is approx, neither faded`() {
        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Exchanging)) as TxHistoryDetailsUM.TwoAssets

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
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Finished)) as TxHistoryDetailsUM.TwoAssets

        // Assert
        assertThat(result.from?.amount?.resolveString()).startsWith("- ")
        assertThat(result.to?.amount?.resolveString()).startsWith("+ ")
        assertThat(result.from?.isFaded).isFalse()
        assertThat(result.to?.isFaded).isFalse()
    }

    @Test
    fun `GIVEN failed express swap WHEN convert THEN both legs faded and signs dropped`() {
        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Failed)) as TxHistoryDetailsUM.TwoAssets

        // Assert
        assertThat(result.from?.isFaded).isTrue()
        assertThat(result.to?.isFaded).isTrue()
        assertThat(result.from?.amount?.resolveString()).doesNotContain("-")
        assertThat(result.to?.amount?.resolveString()).doesNotContain("+")
    }

    @Test
    fun `GIVEN express swap with matched on-chain leg WHEN convert THEN network-fee row from leg`() {
        // Arrange
        val leg = onChain(
            type = TransactionType.Swap,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Finished, txInfo = leg)) as TxHistoryDetailsUM.TwoAssets

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
        ) as TxHistoryDetailsUM.TwoAssets

        // Assert — provider then rate (no on-chain leg, so no fee row).
        assertThat(result.rows.map { it.label }).containsExactly(
            resourceReference(R.string.express_provider),
            resourceReference(R.string.common_rate),
        ).inOrder()
        val providerRow = result.rows.first()
        assertThat(providerRow.label).isEqualTo(resourceReference(R.string.express_provider))
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
        ) as TxHistoryDetailsUM.TwoAssets

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
        ) as TxHistoryDetailsUM.TwoAssets

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
        ) as TxHistoryDetailsUM.TwoAssets

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
        val result = converter.convert(swap) as TxHistoryDetailsUM.TwoAssets

        // Assert — only the provider row remains.
        assertThat(result.rows.map { it.label }).containsExactly(resourceReference(R.string.express_provider))
    }

    @Test
    fun `GIVEN express onramp with both amounts WHEN convert THEN rate row 1 crypto approx fiat`() {
        // Act
        val result = converter.convert(
            expressOnramp(status = ExpressOnrampStatus.Finished),
        ) as TxHistoryDetailsUM.TwoAssets

        // Assert — onramp has no provider in the fixture, so the only row is the rate.
        assertThat(result.rows.map { it.label }).containsExactly(resourceReference(R.string.common_rate))
        val rate = result.rows.first().value.resolveString()
        // 100 SEK / 0.006 BTC ≈ 16,666.67 SEK; base is the resolved crypto symbol (BTC).
        assertThat(rate).startsWith("1")
        assertThat(rate).contains("≈")
        assertThat(rate).contains("BTC")
        assertThat(rate).contains("SEK")
    }

    @Test
    fun `GIVEN finished express onramp WHEN convert THEN paid fiat is unsigned and topped-up crypto is plus`() {
        // Act
        val result = converter.convert(expressOnramp(status = ExpressOnrampStatus.Finished)) as TxHistoryDetailsUM.TwoAssets

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
        val result = converter.convert(expressOnramp(status = ExpressOnrampStatus.Sending)) as TxHistoryDetailsUM.TwoAssets

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

    @Test
    fun `GIVEN finished express onramp WHEN convert THEN TwoAssets with success banner`() {
        // Act
        val result = converter.convert(expressOnramp(status = ExpressOnrampStatus.Finished)) as TxHistoryDetailsUM.TwoAssets

        // Assert
        assertThat(result.header.icon).isEqualTo(TxIcon.Vector(Icons.ic_card_20))
        assertThat(result.statusBanner).isEqualTo(
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
        val result = converter.convert(expressOnramp(status = ExpressOnrampStatus.Verifying)) as TxHistoryDetailsUM.TwoAssets

        // Assert
        assertThat(result.statusBanner).isEqualTo(
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
        // Act
        val result = converter.convert(expressOnramp(status = ExpressOnrampStatus.Unknown)) as TxHistoryDetailsUM.TwoAssets

        // Assert — nothing to surface, the plaque is hidden.
        assertThat(result.statusBanner).isNull()
    }

    @Test
    fun `GIVEN failed express swap with url WHEN convert THEN go-to-provider button opening the url`() {
        // Act
        val result = converter.convert(
            expressSwap(status = ExpressExchangeStatus.Failed, externalTxUrl = EXTERNAL_URL),
        ) as TxHistoryDetailsUM.TwoAssets

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
        ) as TxHistoryDetailsUM.TwoAssets

        // Assert
        assertThat(result.providerButton?.text).isEqualTo(resourceReference(R.string.common_go_to_verification))
    }

    @Test
    fun `GIVEN verifying express onramp with url WHEN convert THEN go-to-verification button opening the url`() {
        // Act
        val result = converter.convert(
            expressOnramp(status = ExpressOnrampStatus.Verifying, externalTxUrl = EXTERNAL_URL),
        ) as TxHistoryDetailsUM.TwoAssets

        // Assert
        val button = result.providerButton
        assertThat(button?.text).isEqualTo(resourceReference(R.string.common_go_to_verification))
        button?.onClick?.invoke()
        assertThat(openedUrls).containsExactly(EXTERNAL_URL)
    }

    @Test
    fun `GIVEN failed express swap without url WHEN convert THEN no provider button`() {
        // Act — the provider supplies no link (e.g. DEX), so there is nowhere to send the user.
        val result = converter.convert(
            expressSwap(status = ExpressExchangeStatus.Failed, externalTxUrl = null),
        ) as TxHistoryDetailsUM.TwoAssets

        // Assert
        assertThat(result.providerButton).isNull()
    }

    @Test
    fun `GIVEN finished express swap with url WHEN convert THEN no provider button`() {
        // Act — a settled success needs no provider action even when a link exists.
        val result = converter.convert(
            expressSwap(status = ExpressExchangeStatus.Finished, externalTxUrl = EXTERNAL_URL),
        ) as TxHistoryDetailsUM.TwoAssets

        // Assert
        assertThat(result.providerButton).isNull()
    }

    // endregion

    // region Express leg owner

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
        val result = ownConverter(lookup).convert(swap) as TxHistoryDetailsUM.TwoAssets

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
        val result = ownConverter(lookup).convert(swap) as TxHistoryDetailsUM.TwoAssets

        // Assert
        val owner = result.to?.owner
        assertThat(owner).isInstanceOf(TxHistoryDetailsUM.AssetOwnerUM.Wallet::class.java)
        assertThat((owner as TxHistoryDetailsUM.AssetOwnerUM.Wallet).name).isEqualTo(stringReference("My Wallet"))
    }

    @Test
    fun `GIVEN send-and-swap to external address WHEN convert THEN to leg owner is external address`() {
        // Arrange — payout address is none of the user's, so it stays an external address.
        val swap = expressSwap(status = ExpressExchangeStatus.Finished, payoutAddress = EXTERNAL_ADDRESS)

        // Act
        val result = ownConverter(lookupOf()).convert(swap) as TxHistoryDetailsUM.TwoAssets

        // Assert
        val owner = result.to?.owner
        assertThat(owner).isInstanceOf(TxHistoryDetailsUM.AssetOwnerUM.Address::class.java)
        assertThat((owner as TxHistoryDetailsUM.AssetOwnerUM.Address).rawAddress).isEqualTo(EXTERNAL_ADDRESS)
        assertThat(result.to?.label).isEqualTo(resourceReference(R.string.common_to))
    }

    @Test
    fun `GIVEN swap without addresses WHEN convert THEN no owner and default labels`() {
        // Act — no fromAddress / payoutAddress plumbed (e.g. very old app version).
        val result = converter.convert(expressSwap(status = ExpressExchangeStatus.Finished))
            as TxHistoryDetailsUM.TwoAssets

        // Assert
        assertThat(result.from?.owner).isNull()
        assertThat(result.to?.owner).isNull()
        assertThat(result.from?.label).isEqualTo(resourceReference(R.string.swapping_from_title_v2))
        assertThat(result.to?.label).isEqualTo(resourceReference(R.string.swapping_to_title))
    }

    @Test
    fun `GIVEN onramp to own account WHEN convert THEN from is You paid and to has account owner`() {
        // Arrange
        val onramp = expressOnramp(status = ExpressOnrampStatus.Finished, payoutAddress = PAYOUT_ADDRESS)
        val lookup = lookupOf(bitcoin.network.id.rawId to mapOf(PAYOUT_ADDRESS to ownAccount))

        // Act
        val result = ownConverter(lookup).convert(onramp) as TxHistoryDetailsUM.TwoAssets

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
        val result = ownConverter(lookup).convert(swap) as TxHistoryDetailsUM.TwoAssets

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
        val result = ownConverter(lookup).convert(swap) as TxHistoryDetailsUM.TwoAssets

        // Assert — ambiguous, so it falls back to the external address rather than guessing an owner.
        val owner = result.from?.owner
        assertThat(owner).isInstanceOf(TxHistoryDetailsUM.AssetOwnerUM.Address::class.java)
        assertThat((owner as TxHistoryDetailsUM.AssetOwnerUM.Address).rawAddress).isEqualTo(FROM_ADDRESS)
    }

    // endregion

    private fun onChain(
        type: TransactionType,
        isOutgoing: Boolean = false,
        status: TxInfo.TransactionStatus = TxInfo.TransactionStatus.Confirmed,
        amount: BigDecimal = BigDecimal.ONE,
        interactionAddressType: TxInfo.InteractionAddressType? = null,
        fee: SdkAmount? = null,
    ): OnChainTx.BSDK = OnChainTx.BSDK(
        TxInfo(
            txHash = TX_HASH,
            timestampInMillis = TIMESTAMP,
            isOutgoing = isOutgoing,
            destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(USER_ADDRESS)),
            sourceType = TxInfo.SourceType.Single(address = USER_ADDRESS),
            interactionAddressType = interactionAddressType,
            status = status,
            type = type,
            amount = amount,
            fee = fee,
        ),
    )

    private fun provider(name: String): ExpressProvider = ExpressProvider(
        providerId = "provider-1",
        name = name,
        type = ExpressProviderType.CEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = null,
    )

    private fun expressSwap(
        status: ExpressExchangeStatus,
        isOutgoing: Boolean = true,
        txInfo: OnChainTx? = null,
        provider: ExpressProvider? = null,
        externalTxUrl: String? = null,
        fromAddress: String? = null,
        payoutAddress: String? = null,
        fromCurrency: CryptoCurrency? = null,
    ): ExpressTx.Swap = ExpressTx.Swap(
        tx = ExchangeTransaction(
            txId = "swap-1",
            status = status,
            createdAtMillis = TIMESTAMP,
            provider = provider,
            payinHash = null,
            payoutHash = null,
            fromAddress = fromAddress,
            payoutAddress = payoutAddress,
            fromAsset = expressAsset(
                networkId = "ethereum",
                amount = BigDecimal("1.5"),
                decimals = 18,
                cryptoCurrency = fromCurrency,
            ),
            toAsset = expressAsset(
                networkId = "bitcoin",
                amount = BigDecimal("0.001"),
                decimals = 8,
                cryptoCurrency = bitcoin,
            ),
            externalTxUrl = externalTxUrl,
        ),
        isOutgoing = isOutgoing,
        txInfo = txInfo,
    )

    private fun expressOnramp(
        status: ExpressOnrampStatus,
        txInfo: OnChainTx? = null,
        externalTxUrl: String? = null,
        payoutAddress: String? = null,
    ): ExpressTx.Onramp = ExpressTx.Onramp(
        tx = OnrampTransaction(
            txId = "onramp-1",
            status = status,
            createdAtMillis = TIMESTAMP,
            provider = null,
            payoutHash = null,
            payoutAddress = payoutAddress,
            externalTxUrl = externalTxUrl,
            fromFiat = Amount(
                currencySymbol = "SEK",
                value = BigDecimal("100"),
                decimals = 2,
                type = AmountType.FiatType(code = "SEK"),
            ),
            toAsset = expressAsset(
                networkId = "bitcoin",
                amount = BigDecimal("0.006"),
                decimals = 8,
                cryptoCurrency = bitcoin,
            ),
        ),
        txInfo = txInfo,
    )

    private fun expressAsset(
        networkId: String,
        amount: BigDecimal,
        decimals: Int,
        cryptoCurrency: CryptoCurrency? = null,
    ): ExpressTransactionAsset =
        ExpressTransactionAsset(
            id = ExpressAssetId(networkId = networkId, contractAddress = "0"),
            amount = amount,
            decimals = decimals,
            cryptoCurrency = cryptoCurrency,
        )

    /** Builds a details lookup with the given per-network own-address maps. */
    private fun lookupOf(
        vararg networks: Pair<Network.RawID, Map<String, Account.CryptoPortfolio>>,
        isAccountsModeEnabled: Boolean = true,
        walletInfoById: Map<UserWalletId, WalletInfo> = mapOf(
            MockAccounts.userWalletId to WalletInfo(
                name = "My Wallet",
                deviceIconUM = DeviceIconUM.Card(mainColor = Color(0xFF1E1E1E), secondColor = null),
            ),
        ),
    ): TxHistoryLookupContext = TxHistoryLookupContext(
        ownAccountByNetwork = networks.toMap(),
        isAccountsModeEnabled = isAccountsModeEnabled,
        walletInfoById = walletInfoById,
    )

    private fun ownConverter(lookup: TxHistoryLookupContext) = TxHistoryInfoToTxHistoryDetailsUMConverter(
        currency = currency,
        onCopyAddress = copiedAddresses::add,
        onGoToProvider = openedUrls::add,
        lookup = lookup,
    )

    private fun TextReference.resolveString(): String = (this as TextReference.Str).value

    private companion object {
        const val TX_HASH = "0xtxhash"
        const val TIMESTAMP = 1_700_000_000_000L
        const val USER_ADDRESS = "0x1234567890abcdef1234"
        const val VALIDATOR_ADDRESS = "0xvalidator"
        const val EXTERNAL_URL = "https://provider.example/tx/swap-1"
        const val FROM_ADDRESS = "0xfromOwnAddress1234"
        const val PAYOUT_ADDRESS = "bc1qPayoutOwnAddress"
        const val EXTERNAL_ADDRESS = "bc1qExternalNonUserAddress"
    }
}