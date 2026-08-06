package com.tangem.features.txhistory.converter

import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference
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
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.WalletInfo
import com.tangem.test.mock.MockAccounts
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.math.BigDecimal

/**
 * Shared fixtures for the transaction-details converter tests: mock currencies/accounts, the on-chain / express-deal
 * builders, the [TxHistoryLookupContext] builder, and the sub-converter factories. Each converter under test
 * ([OnChainTxToDetailsUMConverter], [ExpressTxToDetailsUMConverter], and the [TxHistoryInfoToTxHistoryDetailsUMConverter]
 * dispatcher) has its own `*Test` extending this base.
 */
internal open class TxDetailsConverterTestBase {

    protected val mockCurrencyFactory = MockCryptoCurrencyFactory()
    protected val currency = mockCurrencyFactory.ethereum

    // The express payout leg: a real Bitcoin coin so the resolved symbol (BTC) matches the "bitcoin" network id.
    protected val bitcoin = mockCurrencyFactory.bitcoin
    protected val ownAccount: Account.CryptoPortfolio = MockAccounts.createAccount(derivationIndex = 1, name = "Family")
    protected val secondAccount: Account.CryptoPortfolio =
        MockAccounts.createAccount(derivationIndex = 2, name = "Savings")
    protected val ownPaymentAccount: Account.Payment = Account.Payment(MockAccounts.userWalletId)
    protected val copiedAddresses = mutableListOf<String>()
    protected val openedUrls = mutableListOf<String>()

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

    protected fun onChainConverter(
        menu: ImmutableList<TxHistoryDetailsUM.MenuItemUM> = persistentListOf(),
        validators: List<Yield.Validator> = emptyList(),
        lookup: TxHistoryLookupContext = lookupOf(),
    ) = OnChainTxToDetailsUMConverter(
        currency = currency,
        onCopyAddress = copiedAddresses::add,
        menu = menu,
        validatorsByAddress = validators.associateBy(Yield.Validator::address),
        onOpenValidator = openedUrls::add,
        lookup = lookup,
    )

    protected fun expressConverter(
        lookup: TxHistoryLookupContext = lookupOf(),
        menu: ImmutableList<TxHistoryDetailsUM.MenuItemUM> = persistentListOf(),
    ) = ExpressTxToDetailsUMConverter(
        onGoToProvider = openedUrls::add,
        lookup = lookup,
        menu = menu,
    )

    protected fun txInfo(
        type: TransactionType,
        isOutgoing: Boolean = false,
        status: TxInfo.TransactionStatus = TxInfo.TransactionStatus.Confirmed,
        amount: BigDecimal = BigDecimal.ONE,
        interactionAddressType: TxInfo.InteractionAddressType? = null,
        destinationType: TxInfo.DestinationType =
            TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(USER_ADDRESS)),
        sourceType: TxInfo.SourceType = TxInfo.SourceType.Single(address = USER_ADDRESS),
        fee: SdkAmount? = null,
    ): TxInfo = TxInfo(
        txHash = TX_HASH,
        timestampInMillis = TIMESTAMP,
        isOutgoing = isOutgoing,
        destinationType = destinationType,
        sourceType = sourceType,
        interactionAddressType = interactionAddressType,
        status = status,
        type = type,
        amount = amount,
        fee = fee,
    )

    protected fun onChain(
        type: TransactionType,
        isOutgoing: Boolean = false,
        status: TxInfo.TransactionStatus = TxInfo.TransactionStatus.Confirmed,
        amount: BigDecimal = BigDecimal.ONE,
        interactionAddressType: TxInfo.InteractionAddressType? = null,
        destinationType: TxInfo.DestinationType =
            TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(USER_ADDRESS)),
        fee: SdkAmount? = null,
    ): OnChainTx.BSDK = OnChainTx.BSDK(
        txInfo(
            type = type,
            isOutgoing = isOutgoing,
            status = status,
            amount = amount,
            interactionAddressType = interactionAddressType,
            destinationType = destinationType,
            fee = fee,
        ),
    )

    protected fun validator(
        address: String = VALIDATOR_ADDRESS,
        name: String = "Lido Finance",
        website: String? = VALIDATOR_URL,
    ): Yield.Validator = Yield.Validator(
        address = address,
        status = Yield.Validator.ValidatorStatus.ACTIVE,
        name = name,
        website = website,
        preferred = true,
        isStrategicPartner = false,
    )

    protected fun provider(name: String): ExpressProvider = ExpressProvider(
        providerId = "provider-1",
        name = name,
        type = ExpressProviderType.CEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = null,
    )

    protected fun expressSwap(
        status: ExpressExchangeStatus,
        isOutgoing: Boolean = true,
        txInfo: OnChainTx? = null,
        provider: ExpressProvider? = null,
        externalTxUrl: String? = null,
        fromAddress: String = FROM_ADDRESS,
        payoutAddress: String = PAYOUT_ADDRESS,
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
            externalTxId = null,
            payinAddress = "payin-addr",
            updatedAtMillis = TIMESTAMP,
            refundAssetId = null,
            refundCurrency = null,
            fromAmount = BigDecimal("1.5"),
            toAmount = BigDecimal("0.001"),
            toActualAmount = null,
        ),
        isOutgoing = isOutgoing,
        txInfo = txInfo,
    )

    protected fun expressOnramp(
        status: ExpressOnrampStatus,
        txInfo: OnChainTx? = null,
        externalTxUrl: String? = null,
        payoutAddress: String = PAYOUT_ADDRESS,
        country: OnrampCountry? = null,
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
            country = country,
            toAmount = BigDecimal("0.006"),
            toActualAmount = null,
        ),
        txInfo = txInfo,
    )

    /** A resolved onramp country carrying [flagUrl] as its flag image (the "paid from" flag shown on the fiat leg). */
    protected fun onrampCountry(flagUrl: String): OnrampCountry = OnrampCountry(
        id = "SE",
        name = "Sweden",
        code = "SE",
        image = flagUrl,
        alpha3 = "SWE",
        continent = "Europe",
        defaultCurrency = OnrampCurrency(name = "Swedish Krona", code = "SEK", image = null, precision = 2, unit = "SEK"),
        onrampAvailable = true,
    )

    protected fun expressAsset(
        networkId: String,
        amount: BigDecimal,
        decimals: Int,
        cryptoCurrency: CryptoCurrency? = null,
    ): ExpressTransactionAsset = ExpressTransactionAsset(
        id = ExpressAssetId(networkId = networkId, contractAddress = "0"),
        amount = amount,
        decimals = decimals,
        cryptoCurrency = cryptoCurrency,
    )

    /** Builds a details lookup with the given per-network own-address maps. */
    protected fun lookupOf(
        vararg networks: Pair<Network.RawID, Map<String, Account>>,
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

    /** A `walletInfoById` with the own [MockAccounts.userWalletId] plus a second wallet, so an own-wallet leg has
     * something to disambiguate against. */
    protected fun twoWalletInfo(): Map<UserWalletId, WalletInfo> = mapOf(
        MockAccounts.userWalletId to WalletInfo(name = "My Wallet", deviceIconUM = deviceIcon()),
        UserWalletId("022") to WalletInfo(name = "Second Wallet", deviceIconUM = deviceIcon()),
    )

    private fun deviceIcon(): DeviceIconUM = DeviceIconUM.Card(mainColor = Color(0xFF1E1E1E), secondColor = null)

    protected fun TextReference.resolveString(): String = (this as TextReference.Str).value

    protected companion object {
        const val TX_HASH = "0xtxhash"
        const val TIMESTAMP = 1_700_000_000_000L
        const val USER_ADDRESS = "0x1234567890abcdef1234"
        const val VALIDATOR_ADDRESS = "0xvalidator"
        const val VALIDATOR_URL = "https://lido.fi"
        const val EXTERNAL_URL = "https://provider.example/tx/swap-1"
        const val FROM_ADDRESS = "0xfromOwnAddress1234"
        const val PAYOUT_ADDRESS = "bc1qPayoutOwnAddress"
        const val EXTERNAL_ADDRESS = "bc1qExternalNonUserAddress"
    }
}