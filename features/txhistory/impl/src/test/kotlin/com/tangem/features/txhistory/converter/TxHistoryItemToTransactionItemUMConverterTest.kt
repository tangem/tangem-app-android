package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactAddressLookup
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.Account.Personal.Companion.createMainAccount
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.SdkAmountType
import com.tangem.core.ui.res.generated.icons.ic_percent_20
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.TestContacts
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.WalletInfo
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryItemToTransactionItemUMConverterTest {

    private val txHistoryUiActions: TxHistoryUiActions = mockk(relaxed = true)
    private val coin: CryptoCurrency.Coin = createCoin(symbol = "ETH", decimals = 18)
    private val token: CryptoCurrency.Token = createToken(symbol = "USDT", decimals = 6)

    private val coinConverter
        get() = TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
        )

    private val tokenConverter
        get() = TxHistoryItemToTransactionItemUMConverter(
            currency = token,
            txHistoryUiActions = txHistoryUiActions,
        )

    /** The coin history of a wallet that tracks [token] — the only case where a network-fee row can name its asset. */
    private val networkFeeConverter
        get() = TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
            networkTokens = setOf(token),
        )

    // region Pill dispatch routing

    @Test
    fun `GIVEN Pill TransactionType WHEN convert THEN result is Pill with expected kind`() {
        val cases = listOf(
            TransactionType.Approve(amount = SdkAmount(currencySymbol = "USDT", value = null, decimals = 6), address = USER_ADDRESS) to TransactionItemUM.PillKind.APPROVE,
            TransactionType.Staking.Stake to TransactionItemUM.PillKind.STAKING,
            TransactionType.Staking.Unstake to TransactionItemUM.PillKind.STAKING,
            TransactionType.Staking.Restake to TransactionItemUM.PillKind.STAKING,
            TransactionType.Staking.Vote(validatorAddress = "0xv") to TransactionItemUM.PillKind.STAKING,
            TransactionType.Staking.Withdraw to TransactionItemUM.PillKind.STAKING,
            TransactionType.YieldSupply.Enter(address = USER_ADDRESS) to TransactionItemUM.PillKind.YIELD_MODE,
            TransactionType.YieldSupply.Exit(address = USER_ADDRESS) to TransactionItemUM.PillKind.YIELD_MODE,
            TransactionType.YieldSupply.Topup to TransactionItemUM.PillKind.YIELD_SUPPLY,
            TransactionType.YieldSupply.Send(isYieldSupplyWithdraw = true) to TransactionItemUM.PillKind.YIELD_SUPPLY,
        )

        cases.forEach { (type, expectedKind) ->
            val tx = txInfo(type = type)
            val result = coinConverter.convert(tx)
            assertThat(result).isInstanceOf(TransactionItemUM.Pill::class.java)
            assertThat((result as TransactionItemUM.Pill).kind).isEqualTo(expectedKind)
        }
    }

    // endregion

    // region Content — basic types

    @Test
    fun `GIVEN Operation WHEN convert THEN Content with type name as title`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint NFT"),
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(TextReference.Str("Mint NFT"))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_document_20))
    }

    @Test
    fun `GIVEN Swap confirmed WHEN convert THEN Content with swapped title`() {
        val tx = txInfo(
            type = TransactionType.Swap,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_swapped))
    }

    @Test
    fun `GIVEN Swap unconfirmed WHEN convert THEN Content with swapping title`() {
        val tx = txInfo(
            type = TransactionType.Swap,
            status = TxInfo.TransactionStatus.Unconfirmed,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_swapping))
    }

    @Test
    fun `GIVEN Swap failed WHEN convert THEN Content with swap failed title and directional icon`() {
        val tx = txInfo(
            type = TransactionType.Swap,
            status = TxInfo.TransactionStatus.Failed,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.transaction_history_status_swap_failed))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_down_20))
    }

    @Test
    fun `GIVEN UnknownOperation WHEN convert THEN Content with operation title`() {
        val tx = txInfo(
            type = TransactionType.UnknownOperation,
            isOutgoing = true,
            interactionAddressType = null,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.transaction_history_operation))
        assertThat(result.subtitle).isEqualTo(ContentSubtitle.Plain(TextReference.EMPTY))
    }

    @Test
    fun `GIVEN GaslessFee WHEN convert THEN Content with network fee title`() {
        val tx = txInfo(
            type = TransactionType.GaslessFee,
            interactionAddressType = null,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_network_fee_title))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_percent_20))
    }

    @Test
    fun `GIVEN NetworkFee WHEN convert THEN amount is the network fee it paid`() {
        // The transaction moved no coin, so the row shows its fee rather than its own zero amount
        val tx = txInfo(
            type = TransactionType.NetworkFee(
                amount = SdkAmount(
                    currencySymbol = "USDT",
                    value = BigDecimal("5"),
                    decimals = 6,
                    type = SdkAmountType.Token(contractAddress = TOKEN_CONTRACT),
                ),
                address = EXTERNAL_ADDRESS,
            ),
            amount = BigDecimal.ZERO,
            isOutgoing = true,
            interactionAddressType = null,
        ).copy(fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.000015"), decimals = 18))

        val result = networkFeeConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_network_fee_title))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_percent_20))
        assertThat(result.amount).isEqualTo("${StringsSigns.MINUS}0.000015")
        assertThat(result.subtitle).isEqualTo(
            ContentSubtitle.FeeFor(symbol = "USDT", icon = CryptoCurrencyToIconStateConverter().convert(token)),
        )
    }

    @Test
    fun `GIVEN NetworkFee for an untracked token WHEN convert THEN generic for-sending subtitle`() {
        val tx = txInfo(
            type = TransactionType.NetworkFee(amount = null, address = EXTERNAL_ADDRESS),
            amount = BigDecimal.ZERO,
            isOutgoing = true,
            interactionAddressType = null,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.subtitle).isEqualTo(
            ContentSubtitle.Plain(resRef(R.string.transaction_history_fee_for_sending)),
        )
    }

    @Test
    fun `GIVEN zero-amount Operation in coin history WHEN convert THEN bare network fee row`() {
        // The call moved no coin and was never decoded — the row shows the fee it paid, for the operation itself
        val tx = txInfo(
            type = TransactionType.Operation(name = "YieldSend"),
            amount = BigDecimal.ZERO,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(EXTERNAL_ADDRESS),
        ).copy(fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.000015"), decimals = 18))

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_network_fee_title))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_percent_20))
        assertThat(result.amount).isEqualTo("${StringsSigns.MINUS}0.000015")
        assertThat(result.subtitle).isEqualTo(
            ContentSubtitle.Plain(
                resRef(R.string.transaction_history_fee_for_operation_name, listOf("YieldSend")),
            ),
        )
    }

    @Test
    fun `GIVEN zero-amount UnknownOperation in coin history WHEN convert THEN fee row without a method name`() {
        val tx = txInfo(
            type = TransactionType.UnknownOperation,
            amount = BigDecimal.ZERO,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(EXTERNAL_ADDRESS),
        ).copy(fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.000015"), decimals = 18))

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_network_fee_title))
        assertThat(result.subtitle).isEqualTo(
            ContentSubtitle.Plain(resRef(R.string.transaction_history_fee_for_operation)),
        )
    }

    @Test
    fun `GIVEN incoming zero-amount Operation in coin history WHEN convert THEN stays an operation row`() {
        // Somebody else's call — its fee was paid by the sender, so the row must not report it as the wallet's own
        val tx = txInfo(
            type = TransactionType.Operation(name = "YieldSend"),
            amount = BigDecimal.ZERO,
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(EXTERNAL_ADDRESS),
        ).copy(fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.000015"), decimals = 18))

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(TextReference.Str("YieldSend"))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_document_20))
        assertThat(result.amount).isEqualTo("0.00")
    }

    @Test
    fun `GIVEN zero-amount Operation in token history WHEN convert THEN stays an operation row`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "YieldSend"),
            amount = BigDecimal.ZERO,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(EXTERNAL_ADDRESS),
        ).copy(fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.000015"), decimals = 18))

        val result = tokenConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(TextReference.Str("YieldSend"))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_document_20))
        assertThat(result.amount).isEqualTo("0.00")
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldModeNetworkFee {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: YieldModeFeeModel) {
            // Arrange
            val tx = txInfo(
                type = TransactionType.Operation(name = "SupplyTopUp", recognized = model.recognized),
                amount = BigDecimal.ZERO,
                isOutgoing = true,
                interactionAddressType = TxInfo.InteractionAddressType.Contract(EXTERNAL_ADDRESS),
            ).copy(fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.000015"), decimals = 18))

            // Act
            val result = coinConverter.convert(tx) as TransactionItemUM.Content

            // Assert
            assertThat(result.title).isEqualTo(resRef(R.string.common_network_fee_title))
            assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_percent_20))
            assertThat(result.amount).isEqualTo("${StringsSigns.MINUS}0.000015")
            assertThat(result.subtitle).isEqualTo(ContentSubtitle.Plain(resRef(model.expectedSubtitle)))
        }

        /** The yield operation happened in the token; the coin only paid its gas, and the subtitle says which mode. */
        private fun provideTestModels() = listOf(
            YieldModeFeeModel(
                recognized = TransactionType.YieldSupply.Enter(address = EXTERNAL_ADDRESS),
                expectedSubtitle = R.string.transaction_history_fee_for_yield_mode_activation,
            ),
            YieldModeFeeModel(
                recognized = TransactionType.YieldSupply.Topup,
                expectedSubtitle = R.string.transaction_history_fee_for_yield_mode_top_up,
            ),
            YieldModeFeeModel(
                recognized = TransactionType.YieldSupply.Exit(address = EXTERNAL_ADDRESS),
                expectedSubtitle = R.string.transaction_history_fee_for_yield_mode_deactivation,
            ),
            YieldModeFeeModel(
                recognized = TransactionType.YieldSupply.DeployContract(address = EXTERNAL_ADDRESS),
                expectedSubtitle = R.string.transaction_history_fee_for_yield_mode_deployment,
            ),
            YieldModeFeeModel(
                recognized = TransactionType.YieldSupply.InitializeToken(address = EXTERNAL_ADDRESS),
                expectedSubtitle = R.string.transaction_history_fee_for_yield_mode_initialization,
            ),
            YieldModeFeeModel(
                recognized = TransactionType.YieldSupply.ReactivateToken(address = EXTERNAL_ADDRESS),
                expectedSubtitle = R.string.transaction_history_fee_for_yield_mode_reactivation,
            ),
        )
    }

    internal data class YieldModeFeeModel(
        val recognized: TransactionType.YieldSupply,
        @StringRes val expectedSubtitle: Int,
    )

    @Test
    fun `GIVEN fee-only operation recognized as a yield Send WHEN convert THEN subtitle keeps the method name`() {
        // Arrange — a Send changed no mode, so there is nothing to name but the method itself
        val tx = txInfo(
            type = TransactionType.Operation(
                name = "YieldSend",
                recognized = TransactionType.YieldSupply.Send(isYieldSupplyWithdraw = true),
            ),
            amount = BigDecimal.ZERO,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(EXTERNAL_ADDRESS),
        ).copy(fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.000015"), decimals = 18))

        // Act
        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        // Assert
        assertThat(result.subtitle).isEqualTo(
            ContentSubtitle.Plain(resRef(R.string.transaction_history_fee_for_operation_name, listOf("YieldSend"))),
        )
    }

    @Test
    fun `GIVEN ClaimRewards confirmed WHEN convert THEN Content with reward title and no amount sign`() {
        val tx = txInfo(
            type = TransactionType.Staking.ClaimRewards,
            isOutgoing = false,
            amount = BigDecimal("2.5"),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.transaction_history_status_rewards_claimed))
        assertThat(result.subtitle).isEqualTo(
            ContentSubtitle.Plain(resRef(R.string.transaction_history_earned_from_stake)),
        )
        assertThat(result.amount!!.startsWith(StringsSigns.PLUS)).isFalse()
        assertThat(result.amount!!.startsWith(StringsSigns.MINUS)).isFalse()
    }

    @Test
    fun `GIVEN ClaimRewards unconfirmed WHEN convert THEN Content with claiming title`() {
        val tx = txInfo(
            type = TransactionType.Staking.ClaimRewards,
            status = TxInfo.TransactionStatus.Unconfirmed,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.transaction_history_status_claiming_rewards))
    }

    @Test
    fun `GIVEN ClaimRewards failed WHEN convert THEN Content with rewards claim failed title`() {
        val tx = txInfo(
            type = TransactionType.Staking.ClaimRewards,
            status = TxInfo.TransactionStatus.Failed,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.transaction_history_status_rewards_claim_failed))
    }

    // endregion

    // region Content — Transfer

    @Test
    fun `GIVEN outgoing Transfer confirmed to external address WHEN convert THEN sent title and ExternalAddress subtitle`() {
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_sent))
        assertThat(result.direction).isEqualTo(TransactionItemUM.Content.Direction.OUTGOING)
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_up_20))
        val subtitle = result.subtitle as ContentSubtitle.ExternalAddress
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.TO)
        assertThat(subtitle.rawAddress).isEqualTo(USER_ADDRESS)
    }

    @Test
    fun `GIVEN outgoing Transfer unconfirmed WHEN convert THEN sending title`() {
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            status = TxInfo.TransactionStatus.Unconfirmed,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_sending))
    }

    @Test
    fun `GIVEN outgoing Transfer failed WHEN convert THEN send failed title`() {
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            status = TxInfo.TransactionStatus.Failed,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.transaction_history_status_send_failed))
    }

    @Test
    fun `GIVEN incoming Transfer confirmed WHEN convert THEN received title and FROM subtitle`() {
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_received))
        assertThat(result.direction).isEqualTo(TransactionItemUM.Content.Direction.INCOMING)
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_down_20))
        val subtitle = result.subtitle as ContentSubtitle.ExternalAddress
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.FROM)
    }

    @Test
    fun `GIVEN incoming Transfer unconfirmed WHEN convert THEN receiving title`() {
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = false,
            status = TxInfo.TransactionStatus.Unconfirmed,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_receiving))
    }

    @Test
    fun `GIVEN incoming Transfer failed WHEN convert THEN receive failed title`() {
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = false,
            status = TxInfo.TransactionStatus.Failed,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.transaction_history_status_receive_failed))
    }

    @Test
    fun `GIVEN Transfer with own account in accounts mode WHEN convert THEN OwnAccount subtitle and transferred title`() {
        val ownAccount = createMainAccount(UserWalletId(stringValue = "00"))
        val converter = TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
            lookupContext = TxHistoryLookupContext(
                ownAccountByNetwork = mapOf(coin.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
                isAccountsModeEnabled = true,
                walletInfoById = emptyMap(),
            ),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = converter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_transferred))
        val subtitle = result.subtitle as ContentSubtitle.OwnAccount
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.TO)
        assertThat(subtitle.avatar).isEqualTo(
            ContentSubtitle.PortfolioAvatar.Resource(
                resId = ownAccount.icon.value.getResId(),
                color = ownAccount.icon.color.getUiColor(),
            ),
        )
    }

    @Test
    fun `GIVEN own account with a letter icon WHEN convert THEN OwnAccount subtitle carries the name monogram`() {
        // Arrange — an account that kept the default letter icon must render its name, not the letter placeholder glyph.
        val ownAccount = createMainAccount(UserWalletId(stringValue = "00")).copy(
            icon = CryptoPortfolioIcon.ofCustomAccount(
                value = CryptoPortfolioIcon.Icon.Letter,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
        )
        val converter = TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
            lookupContext = TxHistoryLookupContext(
                ownAccountByNetwork = mapOf(coin.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
                isAccountsModeEnabled = true,
                walletInfoById = emptyMap(),
            ),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val result = converter.convert(tx) as TransactionItemUM.Content

        // Assert
        val subtitle = result.subtitle as ContentSubtitle.OwnAccount
        assertThat(subtitle.avatar).isEqualTo(
            ContentSubtitle.PortfolioAvatar.Monogram(
                name = ownAccount.accountName.toUM().value,
                color = CryptoPortfolioIcon.Color.Azure.getUiColor(),
            ),
        )
    }

    @Test
    fun `GIVEN Transfer to own Payment account in accounts mode WHEN convert THEN OwnPaymentAccount subtitle`() {
        val ownPaymentAccount = Account.Payment(UserWalletId(stringValue = "00"))
        val converter = TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
            lookupContext = TxHistoryLookupContext(
                ownAccountByNetwork = mapOf(coin.network.id.rawId to mapOf(USER_ADDRESS to ownPaymentAccount)),
                isAccountsModeEnabled = true,
                walletInfoById = emptyMap(),
            ),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = converter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_transferred))
        val subtitle = result.subtitle as ContentSubtitle.OwnPaymentAccount
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.TO)
        assertThat(subtitle.accountName).isEqualTo(ownPaymentAccount.accountName.toUM().value)
    }

    @Test
    fun `GIVEN Transfer with own account in wallets mode WHEN convert THEN OwnWallet subtitle`() {
        val userWalletId = UserWalletId(stringValue = "01")
        val ownAccount = createMainAccount(userWalletId)
        val walletInfo = WalletInfo(name = "Main wallet", deviceIconUM = DeviceIconUM.Mobile)
        val converter = TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
            lookupContext = TxHistoryLookupContext(
                ownAccountByNetwork = mapOf(coin.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
                isAccountsModeEnabled = false,
                walletInfoById = mapOf(userWalletId to walletInfo),
            ),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = converter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_transferred))
        val subtitle = result.subtitle as ContentSubtitle.OwnWallet
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.FROM)
        assertThat(subtitle.walletName).isEqualTo("Main wallet")
        assertThat(subtitle.deviceIconUM).isEqualTo(DeviceIconUM.Mobile)
    }

    @Test
    fun `GIVEN outgoing Transfer to a saved contact WHEN convert THEN Contact subtitle with monogram`() {
        // Arrange
        val converter = contactConverter(contacts = listOf(SAVED_CONTACT))
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(EXTERNAL_ADDRESS),
        )

        // Act
        val result = converter.convert(tx) as TransactionItemUM.Content

        // Assert
        assertThat(result.title).isEqualTo(resRef(R.string.common_sent))
        val subtitle = result.subtitle as ContentSubtitle.Contact
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.TO)
        assertThat(subtitle.contactName).isEqualTo("Binance")
        assertThat((subtitle.avatar as ContentSubtitle.PortfolioAvatar.Monogram).name)
            .isEqualTo(stringReference("Binance"))
    }

    @Test
    fun `GIVEN outgoing Transfer to a contact with a picked icon WHEN convert THEN Contact subtitle with resource`() {
        // Arrange
        val contact = TestContacts.create(
            name = "Mom",
            address = EXTERNAL_ADDRESS,
            walletId = OWN_WALLET_ID,
            networkId = coin.network.id.rawId,
            icon = CryptoPortfolioIcon.Icon.User.name,
        )
        val converter = contactConverter(contacts = listOf(contact))
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(EXTERNAL_ADDRESS),
        )

        // Act
        val result = converter.convert(tx) as TransactionItemUM.Content

        // Assert
        val subtitle = result.subtitle as ContentSubtitle.Contact
        assertThat(subtitle.contactName).isEqualTo("Mom")
        assertThat((subtitle.avatar as ContentSubtitle.PortfolioAvatar.Resource).resId)
            .isEqualTo(CryptoPortfolioIcon.Icon.User.getResId())
    }

    @Test
    fun `GIVEN outgoing Operation to a saved contact WHEN convert THEN Contact subtitle`() {
        // Arrange — an unrecognized contract call, not a transfer: the address book is consulted all the same.
        val converter = contactConverter(contacts = listOf(SAVED_CONTACT))
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint NFT"),
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(EXTERNAL_ADDRESS),
        )

        // Act
        val result = converter.convert(tx) as TransactionItemUM.Content

        // Assert
        val subtitle = result.subtitle as ContentSubtitle.Contact
        assertThat(subtitle.contactName).isEqualTo("Binance")
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.TO)
    }

    @Test
    fun `GIVEN incoming Operation from a saved contact WHEN convert THEN Contact subtitle`() {
        // Arrange — an incoming call reads its sender from sourceType, which is resolved like any counterparty.
        val converter = contactConverter(contacts = listOf(SAVED_CONTACT))
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint NFT"),
            isOutgoing = false,
            sourceType = TxInfo.SourceType.Single(EXTERNAL_ADDRESS),
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val result = converter.convert(tx) as TransactionItemUM.Content

        // Assert
        val subtitle = result.subtitle as ContentSubtitle.Contact
        assertThat(subtitle.contactName).isEqualTo("Binance")
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.FROM)
    }

    @Test
    fun `GIVEN incoming Transfer from a saved contact WHEN convert THEN Contact subtitle`() {
        // Arrange
        val converter = contactConverter(contacts = listOf(SAVED_CONTACT))
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(EXTERNAL_ADDRESS),
        )

        // Act
        val result = converter.convert(tx) as TransactionItemUM.Content

        // Assert — the sender is named after the contact, same as a recipient.
        val subtitle = result.subtitle as ContentSubtitle.Contact
        assertThat(subtitle.contactName).isEqualTo("Binance")
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.FROM)
    }

    @Test
    fun `GIVEN Transfer with own account but missing wallet info in wallets mode WHEN convert THEN ExternalAddress subtitle`() {
        val ownAccount = createMainAccount(UserWalletId(stringValue = "02"))
        val converter = TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
            lookupContext = TxHistoryLookupContext(
                ownAccountByNetwork = mapOf(coin.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
                isAccountsModeEnabled = false,
                walletInfoById = emptyMap(),
            ),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = converter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_sent))
        assertThat(result.subtitle).isInstanceOf(ContentSubtitle.ExternalAddress::class.java)
    }

    @Test
    fun `GIVEN Transfer with non-User interaction WHEN convert THEN PlainAddress subtitle`() {
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.subtitle).isInstanceOf(ContentSubtitle.PlainAddress::class.java)
        assertThat(result.title).isEqualTo(resRef(R.string.common_sent))
    }

    // endregion

    // region Content — Operation / UnknownOperation reclassified as own transfer

    @Test
    fun `GIVEN incoming Operation whose sender is own WHEN convert THEN rendered as own transfer`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = false,
            sourceAddress = USER_ADDRESS,
            destinationAddress = EXTERNAL_ADDRESS,
        )

        val result = ownAccountConverter(USER_ADDRESS).convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_transferred))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_down_20))
        val subtitle = result.subtitle as ContentSubtitle.OwnAccount
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.FROM)
    }

    @Test
    fun `GIVEN outgoing UnknownOperation whose recipient is own WHEN convert THEN rendered as own transfer`() {
        val tx = txInfo(
            type = TransactionType.UnknownOperation,
            isOutgoing = true,
            sourceAddress = EXTERNAL_ADDRESS,
            destinationAddress = USER_ADDRESS,
        )

        val result = ownAccountConverter(USER_ADDRESS).convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_transferred))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_up_20))
        val subtitle = result.subtitle as ContentSubtitle.OwnAccount
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.TO)
    }

    @Test
    fun `GIVEN incoming Operation own only on destination side WHEN convert THEN stays Operation`() {
        // The trap: for an incoming tx the counterparty is the sender; the own destination must not trigger reclassification.
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = false,
            sourceAddress = EXTERNAL_ADDRESS,
            destinationAddress = USER_ADDRESS,
        )

        val result = ownAccountConverter(USER_ADDRESS).convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(TextReference.Str("Mint"))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_document_20))
    }

    @Test
    fun `GIVEN Operation with own counterparty but no lookup context WHEN convert THEN stays Operation`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            destinationAddress = USER_ADDRESS,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(TextReference.Str("Mint"))
    }

    // endregion

    // region Content — YieldSupply

    @Test
    fun `GIVEN YieldSupply Topup WHEN convert THEN supplied pill naming Aave`() {
        val tx = txInfo(type = TransactionType.YieldSupply.Topup)

        val result = coinConverter.convert(tx) as TransactionItemUM.Pill

        assertThat(result.kind).isEqualTo(TransactionItemUM.PillKind.YIELD_SUPPLY)
        assertThat(result.label).isEqualTo(resRef(R.string.yield_module_transaction_supplied))
        assertThat(result.subtitle).isEqualTo(
            TransactionItemUM.PillSubtitle.Provider(
                direction = ContentSubtitle.Direction.TO,
                name = resRef(R.string.yield_module_provider),
                iconResId = R.drawable.img_aave_22,
            ),
        )
    }

    @Test
    fun `GIVEN YieldSupply Send not withdraw and incoming WHEN convert THEN received title`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = false,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_received))
    }

    @Test
    fun `GIVEN YieldSupply Send not withdraw and outgoing WHEN convert THEN sent title`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = true,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_sent))
    }

    @Test
    fun `GIVEN YieldSupply Send not withdraw and unconfirmed WHEN convert THEN receiving title`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            status = TxInfo.TransactionStatus.Unconfirmed,
            isOutgoing = false,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_receiving))
    }

    @Test
    fun `GIVEN YieldSupply Send withdraw WHEN convert THEN withdrawn pill naming Aave`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = true),
            isOutgoing = false,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Pill

        assertThat(result.kind).isEqualTo(TransactionItemUM.PillKind.YIELD_SUPPLY)
        assertThat(result.label).isEqualTo(resRef(R.string.transaction_history_withdrawn))
        assertThat(result.subtitle).isEqualTo(
            TransactionItemUM.PillSubtitle.Provider(
                direction = ContentSubtitle.Direction.FROM,
                name = resRef(R.string.yield_module_provider),
                iconResId = R.drawable.img_aave_22,
            ),
        )
    }

    @Test
    fun `GIVEN YieldSupply Send withdraw and unconfirmed WHEN convert THEN withdrawing pill`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = true),
            status = TxInfo.TransactionStatus.Unconfirmed,
            isOutgoing = false,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Pill

        assertThat(result.label).isEqualTo(resRef(R.string.common_withdrawing))
    }

    @Test
    fun `GIVEN YieldSupply Send not withdraw to external address WHEN convert THEN ExternalAddress subtitle`() {
        // A non-withdraw Send is an ordinary transfer: an unresolved counterparty renders as the external address.
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.ExternalAddress
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.TO)
        assertThat(subtitle.rawAddress).isEqualTo(USER_ADDRESS)
    }

    @Test
    fun `GIVEN YieldSupply Send not withdraw to own account WHEN convert THEN OwnAccount subtitle`() {
        // The reported bug: a non-withdraw Send to the user's own account must resolve the owner in the list too,
        // matching the details screen — not fall back to a raw address.
        val ownAccount = createMainAccount(UserWalletId(stringValue = "00"))
        val converter = TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
            lookupContext = TxHistoryLookupContext(
                ownAccountByNetwork = mapOf(coin.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
                isAccountsModeEnabled = true,
                walletInfoById = emptyMap(),
            ),
        )
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = converter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.OwnAccount
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.TO)
    }

    @Test
    fun `GIVEN outgoing YieldSupply Send to own account WHEN convert THEN transferred title`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = ownAccountConverter(USER_ADDRESS).convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_transferred))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_up_20))
    }

    @Test
    fun `GIVEN incoming YieldSupply Send from own account WHEN convert THEN transferred title`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = ownAccountConverter(USER_ADDRESS).convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_transferred))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_down_20))
    }

    @Test
    fun `GIVEN YieldSupply Send Token incoming WHEN convert THEN amount and symbol shown`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = false,
        )

        val result = tokenConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.amount).isNotEmpty()
        assertThat(result.currencySymbol).isEqualTo("USDT")
    }

    @Test
    fun `GIVEN YieldSupply DeployContract WHEN convert THEN deploy title and doc icon`() {
        val tx = txInfo(type = TransactionType.YieldSupply.DeployContract(address = USER_ADDRESS))

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.yield_module_transaction_deploy_contract))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_document_20))
    }

    @Test
    fun `GIVEN YieldSupply InitializeToken WHEN convert THEN initialize title and gear icon`() {
        val tx = txInfo(type = TransactionType.YieldSupply.InitializeToken(address = USER_ADDRESS))

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.yield_module_transaction_initialize))
        assertThat(result.icon).isEqualTo(TxIcon.Res(R.drawable.ic_gear_24))
    }

    @Test
    fun `GIVEN YieldSupply ReactivateToken WHEN convert THEN reactivate title and refresh icon`() {
        val tx = txInfo(type = TransactionType.YieldSupply.ReactivateToken(address = USER_ADDRESS))

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.yield_module_transaction_reactivate))
        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_refresh_20))
    }

    @Test
    fun `GIVEN YieldSupply Topup Token WHEN convert THEN pill carries no amount`() {
        // The design states the operation and its provider only — the moved value lives in the details card.
        val tx = txInfo(type = TransactionType.YieldSupply.Topup, amount = BigDecimal("3.0"))

        val result = tokenConverter.convert(tx) as TransactionItemUM.Pill

        assertThat(result.amount).isNull()
        assertThat(result.currencySymbol).isNull()
    }

    @Test
    fun `GIVEN YieldSupply Topup Coin with a user address WHEN convert THEN pill names Aave, not the address`() {
        // Arrange — a coin topup names the provider too: the pill mirrors a withdraw, which never shows the address.
        val tx = txInfo(
            type = TransactionType.YieldSupply.Topup,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
            isOutgoing = true,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Pill

        assertThat(result.subtitle).isEqualTo(
            TransactionItemUM.PillSubtitle.Provider(
                direction = ContentSubtitle.Direction.TO,
                name = resRef(R.string.yield_module_provider),
                iconResId = R.drawable.img_aave_22,
            ),
        )
    }

    // endregion

    // region Amount formatting

    @Test
    fun `GIVEN outgoing confirmed WHEN convert THEN amount has minus prefix`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            amount = BigDecimal("1.5"),
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.amount!!.startsWith(StringsSigns.MINUS)).isTrue()
    }

    @Test
    fun `GIVEN incoming confirmed WHEN convert THEN amount has plus prefix`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = false,
            amount = BigDecimal("1.5"),
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.amount!!.startsWith(StringsSigns.PLUS)).isTrue()
    }

    @Test
    fun `GIVEN failed Operation WHEN convert THEN amount has no sign prefix`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            status = TxInfo.TransactionStatus.Failed,
            amount = BigDecimal("1.5"),
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.amount!!.startsWith(StringsSigns.MINUS)).isFalse()
        assertThat(result.amount!!.startsWith(StringsSigns.PLUS)).isFalse()
    }

    @Test
    fun `GIVEN zero amount Operation WHEN convert THEN amount has no sign prefix`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            amount = BigDecimal.ZERO,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.amount!!.startsWith(StringsSigns.MINUS)).isFalse()
        assertThat(result.amount!!.startsWith(StringsSigns.PLUS)).isFalse()
    }

    // endregion

    // region Address subtitle resolution

    @Test
    fun `GIVEN outgoing Operation with Contract interaction WHEN convert THEN contract address subtitle`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.PlainAddress
        val res = subtitle.text as TextReference.Res
        assertThat(res.id).isEqualTo(R.string.transaction_history_contract_address)
    }

    @Test
    fun `GIVEN Operation with Multiple interaction outgoing WHEN convert THEN to-address subtitle`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.Multiple(
                addresses = listOf(USER_ADDRESS, "0xother"),
            ),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.Plain
        val res = subtitle.text as TextReference.Res
        assertThat(res.id).isEqualTo(R.string.transaction_history_transaction_to_address)
    }

    @Test
    fun `GIVEN incoming Operation WHEN convert THEN subtitle shows the real sender from source`() {
        // interactionAddressType is the own destination on an incoming call; the subtitle must show the actual sender.
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = false,
            sourceAddress = EXTERNAL_ADDRESS,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.ExternalAddress
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.FROM)
        assertThat(subtitle.rawAddress).isEqualTo(EXTERNAL_ADDRESS)
    }

    @Test
    fun `GIVEN incoming Operation with multiple senders WHEN convert THEN from-multiple-addresses subtitle`() {
        // interactionAddressType is the own destination on an incoming call, so a Multiple source must render
        // "from multiple addresses" instead of falling back to the own destination address.
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = false,
            sourceType = TxInfo.SourceType.Multiple(addresses = listOf(EXTERNAL_ADDRESS, USER_ADDRESS)),
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.subtitle).isEqualTo(
            ContentSubtitle.Plain(
                resRef(
                    R.string.transaction_history_transaction_from_address,
                    listOf(resRef(R.string.transaction_history_multiple_addresses)),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN outgoing Operation with Validator interaction WHEN convert THEN validator subtitle`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.Validator(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.PlainAddress
        val res = subtitle.text as TextReference.Res
        assertThat(res.id).isEqualTo(R.string.transaction_history_transaction_validator)
    }

    @Test
    fun `GIVEN outgoing Operation with null interaction WHEN convert THEN empty subtitle`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            interactionAddressType = null,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.subtitle).isEqualTo(ContentSubtitle.Plain(TextReference.EMPTY))
    }

    // endregion

    // region Misc

    @Test
    fun `GIVEN failed Transfer WHEN convert THEN directional icon kept`() {
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            status = TxInfo.TransactionStatus.Failed,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_up_20))
    }

    @Test
    fun `GIVEN any Content WHEN onClick invoked THEN openTxInExplorer called with txHash`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content
        result.onClick()

        verify { txHistoryUiActions.openTxInExplorer(TX_HASH) }
    }

    @Test
    fun `GIVEN tx WHEN convert THEN txHash and timestamp propagated`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.txHash).isEqualTo(TX_HASH)
        assertThat(result.timestamp).isEqualTo(TIMESTAMP)
    }

    // endregion

    // region Helpers

    private fun txInfo(
        type: TransactionType,
        status: TxInfo.TransactionStatus = TxInfo.TransactionStatus.Confirmed,
        isOutgoing: Boolean = false,
        amount: BigDecimal = BigDecimal.ONE,
        interactionAddressType: TxInfo.InteractionAddressType? = null,
        sourceAddress: String = USER_ADDRESS,
        destinationAddress: String = USER_ADDRESS,
        sourceType: TxInfo.SourceType = TxInfo.SourceType.Single(address = sourceAddress),
    ): TxInfo.Single = TxInfo.Single(
        txHash = TX_HASH,
        timestampInMillis = TIMESTAMP,
        isOutgoing = isOutgoing,
        destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(destinationAddress)),
        sourceType = sourceType,
        interactionAddressType = interactionAddressType,
        status = status,
        type = type,
        amount = amount,
    )

    private fun ownAccountConverter(ownAddress: String): TxHistoryItemToTransactionItemUMConverter =
        TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
            lookupContext = TxHistoryLookupContext(
                ownAccountByNetwork = mapOf(coin.network.id.rawId to mapOf(ownAddress to createMainAccount(OWN_WALLET_ID))),
                isAccountsModeEnabled = true,
                walletInfoById = emptyMap(),
            ),
        )

    private fun resRef(id: Int): TextReference = TextReference.Res(id = id)

    private fun contactConverter(contacts: List<Contact>) = TxHistoryItemToTransactionItemUMConverter(
        currency = coin,
        txHistoryUiActions = txHistoryUiActions,
        lookupContext = TxHistoryLookupContext(
            ownAccountByNetwork = emptyMap(),
            isAccountsModeEnabled = true,
            walletInfoById = emptyMap(),
            contacts = ContactAddressLookup.of(contacts),
            viewedWalletId = OWN_WALLET_ID,
        ),
    )

    private fun resRef(id: Int, args: List<Any>): TextReference = TextReference.Res(
        id = id,
        formatArgs = com.tangem.core.ui.extensions.WrappedList(args),
    )

    private fun createCoin(symbol: String, decimals: Int): CryptoCurrency.Coin = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "ethereum"),
        ),
        network = createNetwork(symbol = symbol, canHandleTokens = true),
        name = "Ethereum",
        symbol = symbol,
        decimals = decimals,
        iconUrl = null,
        isCustom = false,
    )

    private fun createToken(symbol: String, decimals: Int): CryptoCurrency.Token = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "ethereum"),
            suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = TOKEN_CONTRACT),
        ),
        network = createNetwork(symbol = "ETH", canHandleTokens = true),
        name = "Tether USD",
        symbol = symbol,
        decimals = decimals,
        iconUrl = null,
        isCustom = false,
        contractAddress = TOKEN_CONTRACT,
    )

    private fun createNetwork(symbol: String, canHandleTokens: Boolean): Network = Network(
        id = Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None),
        name = "Ethereum",
        currencySymbol = symbol,
        derivationPath = Network.DerivationPath.None,
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        hasFiatFeeRate = true,
        canHandleTokens = canHandleTokens,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )

    private companion object {
        const val TX_HASH = "0xtxhash"
        const val TIMESTAMP = 1_700_000_000_000L
        const val USER_ADDRESS = "0x1234567890abcdef1234"
        const val EXTERNAL_ADDRESS = "0xExternalAddress5678"
        const val TOKEN_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        val OWN_WALLET_ID = UserWalletId(stringValue = "00")
        val SAVED_CONTACT = TestContacts.create(
            name = "Binance",
            address = EXTERNAL_ADDRESS,
            walletId = OWN_WALLET_ID,
            networkId = Network.RawID(value = "ethereum"),
        )
    }

    // endregion
}