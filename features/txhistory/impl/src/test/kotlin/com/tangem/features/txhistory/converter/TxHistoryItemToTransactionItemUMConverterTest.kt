package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.Account.CryptoPortfolio.Companion.createMainAccount
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.WalletInfo
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.StringsSigns
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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

    // region Pill dispatch routing

    @Test
    fun `GIVEN Pill TransactionType WHEN convert THEN result is Pill with expected kind`() {
        val cases = listOf(
            TransactionType.Approve to TransactionItemUM.PillKind.APPROVE,
            TransactionType.Staking.Stake to TransactionItemUM.PillKind.STAKING,
            TransactionType.Staking.Unstake to TransactionItemUM.PillKind.STAKING,
            TransactionType.Staking.Restake to TransactionItemUM.PillKind.STAKING,
            TransactionType.Staking.Vote(validatorAddress = "0xv") to TransactionItemUM.PillKind.STAKING,
            TransactionType.Staking.Withdraw to TransactionItemUM.PillKind.STAKING,
            TransactionType.YieldSupply.Enter(address = USER_ADDRESS) to TransactionItemUM.PillKind.YIELD_MODE,
            TransactionType.YieldSupply.Exit(address = USER_ADDRESS) to TransactionItemUM.PillKind.YIELD_MODE,
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
        assertThat(result.iconRes).isEqualTo(R.drawable.ic_arrow_down_24)
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
    fun `GIVEN Swap failed WHEN convert THEN Content with composed failed title and close icon`() {
        val tx = txInfo(
            type = TransactionType.Swap,
            status = TxInfo.TransactionStatus.Failed,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(
            resRef(R.string.common_action_failed, listOf(resRef(R.string.common_swapping))),
        )
        assertThat(result.iconRes).isEqualTo(R.drawable.ic_close_24)
    }

    @Test
    fun `GIVEN UnknownOperation WHEN convert THEN Content with operation title`() {
        val tx = txInfo(
            type = TransactionType.UnknownOperation,
            interactionAddressType = null,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.transaction_history_operation))
        assertThat(result.subtitle).isEqualTo(ContentSubtitle.Plain(TextReference.EMPTY))
    }

    @Test
    fun `GIVEN GaslessFee WHEN convert THEN Content with gasless fee title`() {
        val tx = txInfo(
            type = TransactionType.GaslessFee,
            interactionAddressType = null,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.gasless_transaction_fee))
    }

    @Test
    fun `GIVEN ClaimRewards confirmed WHEN convert THEN Content with reward title and no amount sign`() {
        val tx = txInfo(
            type = TransactionType.Staking.ClaimRewards,
            isOutgoing = false,
            amount = BigDecimal("2.5"),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.transaction_history_staking_reward))
        assertThat(result.subtitle).isEqualTo(
            ContentSubtitle.Plain(resRef(R.string.transaction_history_earned_from_stake)),
        )
        assertThat(result.amount.startsWith(StringsSigns.PLUS)).isFalse()
        assertThat(result.amount.startsWith(StringsSigns.MINUS)).isFalse()
    }

    @Test
    fun `GIVEN ClaimRewards unconfirmed WHEN convert THEN Content with claiming title`() {
        val tx = txInfo(
            type = TransactionType.Staking.ClaimRewards,
            status = TxInfo.TransactionStatus.Unconfirmed,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.transaction_history_claiming_reward))
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
        assertThat(result.iconRes).isEqualTo(R.drawable.ic_arrow_up_24)
        val subtitle = result.subtitle as ContentSubtitle.ExternalAddress
        assertThat(subtitle.direction).isEqualTo(ContentSubtitle.Direction.TO)
        assertThat(subtitle.rawAddress).isEqualTo(USER_ADDRESS)
        assertThat(subtitle.briefAddress).isEqualTo(USER_ADDRESS_BRIEF)
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
    fun `GIVEN outgoing Transfer failed WHEN convert THEN composed failed title`() {
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            status = TxInfo.TransactionStatus.Failed,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(
            resRef(R.string.common_action_failed, listOf(resRef(R.string.common_sending))),
        )
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
        assertThat(result.iconRes).isEqualTo(R.drawable.ic_arrow_down_24)
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
    fun `GIVEN Transfer with own account in accounts mode WHEN convert THEN OwnAccount subtitle and transferred title`() {
        val ownAccount = createMainAccount(UserWalletId(stringValue = "00"))
        val converter = TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
            lookupContext = TxHistoryLookupContext(
                ownAccountByAddress = mapOf(USER_ADDRESS to ownAccount),
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
        assertThat(subtitle.iconResId).isNotEqualTo(0)
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
                ownAccountByAddress = mapOf(USER_ADDRESS to ownAccount),
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
    fun `GIVEN Transfer with own account but missing wallet info in wallets mode WHEN convert THEN ExternalAddress subtitle`() {
        val ownAccount = createMainAccount(UserWalletId(stringValue = "02"))
        val converter = TxHistoryItemToTransactionItemUMConverter(
            currency = coin,
            txHistoryUiActions = txHistoryUiActions,
            lookupContext = TxHistoryLookupContext(
                ownAccountByAddress = mapOf(USER_ADDRESS to ownAccount),
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

    // region Content — YieldSupply

    @Test
    fun `GIVEN YieldSupply Topup WHEN convert THEN topup title`() {
        val tx = txInfo(type = TransactionType.YieldSupply.Topup)

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.yield_module_transaction_topup))
    }

    @Test
    fun `GIVEN YieldSupply Send Coin not withdraw and incoming WHEN convert THEN transfer title`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = false,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.common_transfer))
    }

    @Test
    fun `GIVEN YieldSupply Send Coin withdraw WHEN convert THEN withdraw title`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = true),
            isOutgoing = false,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.yield_module_transaction_withdraw))
    }

    @Test
    fun `GIVEN YieldSupply Send outgoing WHEN convert THEN withdraw title`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = true,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.yield_module_transaction_withdraw))
    }

    @Test
    fun `GIVEN YieldSupply Send Token incoming WHEN convert THEN amount and symbol hidden`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = false,
        )

        val result = tokenConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.amount).isEmpty()
        assertThat(result.currencySymbol).isEmpty()
    }

    @Test
    fun `GIVEN YieldSupply Send Token outgoing WHEN convert THEN amount and symbol shown`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = true),
            isOutgoing = true,
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
        assertThat(result.iconRes).isEqualTo(R.drawable.ic_doc_24)
    }

    @Test
    fun `GIVEN YieldSupply InitializeToken WHEN convert THEN initialize title and gear icon`() {
        val tx = txInfo(type = TransactionType.YieldSupply.InitializeToken(address = USER_ADDRESS))

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.yield_module_transaction_initialize))
        assertThat(result.iconRes).isEqualTo(R.drawable.ic_gear_24)
    }

    @Test
    fun `GIVEN YieldSupply ReactivateToken WHEN convert THEN reactivate title and refresh icon`() {
        val tx = txInfo(type = TransactionType.YieldSupply.ReactivateToken(address = USER_ADDRESS))

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.title).isEqualTo(resRef(R.string.yield_module_transaction_reactivate))
        assertThat(result.iconRes).isEqualTo(R.drawable.ic_refresh_24)
    }

    @Test
    fun `GIVEN YieldSupply Topup Token WHEN convert THEN amount-formatted topup subtitle`() {
        val tx = txInfo(type = TransactionType.YieldSupply.Topup, amount = BigDecimal("3.0"))

        val result = tokenConverter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.Plain
        val res = subtitle.text as TextReference.Res
        assertThat(res.id).isEqualTo(R.string.yield_module_transaction_topup_subtitle)
    }

    @Test
    fun `GIVEN YieldSupply Send Token withdraw incoming WHEN convert THEN exit subtitle`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = true),
            isOutgoing = false,
        )

        val result = tokenConverter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.Plain
        val res = subtitle.text as TextReference.Res
        assertThat(res.id).isEqualTo(R.string.yield_module_transaction_exit_subtitle)
    }

    @Test
    fun `GIVEN YieldSupply Topup Coin WHEN convert THEN address-based subtitle`() {
        val tx = txInfo(
            type = TransactionType.YieldSupply.Topup,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
            isOutgoing = true,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.Plain
        val res = subtitle.text as TextReference.Res
        assertThat(res.id).isEqualTo(R.string.transaction_history_transaction_for_address)
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

        assertThat(result.amount.startsWith(StringsSigns.MINUS)).isTrue()
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

        assertThat(result.amount.startsWith(StringsSigns.PLUS)).isTrue()
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

        assertThat(result.amount.startsWith(StringsSigns.MINUS)).isFalse()
        assertThat(result.amount.startsWith(StringsSigns.PLUS)).isFalse()
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

        assertThat(result.amount.startsWith(StringsSigns.MINUS)).isFalse()
        assertThat(result.amount.startsWith(StringsSigns.PLUS)).isFalse()
    }

    // endregion

    // region Address subtitle resolution

    @Test
    fun `GIVEN Operation with Contract interaction WHEN convert THEN contract address subtitle`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
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
    fun `GIVEN Operation with Multiple interaction incoming WHEN convert THEN from-address subtitle`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.Multiple(
                addresses = listOf(USER_ADDRESS),
            ),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.Plain
        val res = subtitle.text as TextReference.Res
        assertThat(res.id).isEqualTo(R.string.transaction_history_transaction_from_address)
    }

    @Test
    fun `GIVEN Operation with Validator interaction WHEN convert THEN validator subtitle`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            interactionAddressType = TxInfo.InteractionAddressType.Validator(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        val subtitle = result.subtitle as ContentSubtitle.PlainAddress
        val res = subtitle.text as TextReference.Res
        assertThat(res.id).isEqualTo(R.string.transaction_history_transaction_validator)
    }

    @Test
    fun `GIVEN Operation with null interaction WHEN convert THEN empty subtitle`() {
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            interactionAddressType = null,
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.subtitle).isEqualTo(ContentSubtitle.Plain(TextReference.EMPTY))
    }

    // endregion

    // region Misc

    @Test
    fun `GIVEN failed Transfer WHEN convert THEN icon overridden to close`() {
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            status = TxInfo.TransactionStatus.Failed,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = coinConverter.convert(tx) as TransactionItemUM.Content

        assertThat(result.iconRes).isEqualTo(R.drawable.ic_close_24)
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
    ): TxInfo = TxInfo(
        txHash = TX_HASH,
        timestampInMillis = TIMESTAMP,
        isOutgoing = isOutgoing,
        destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(USER_ADDRESS)),
        sourceType = TxInfo.SourceType.Single(address = USER_ADDRESS),
        interactionAddressType = interactionAddressType,
        status = status,
        type = type,
        amount = amount,
    )

    private fun resRef(id: Int): TextReference = TextReference.Res(id = id)

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
        const val USER_ADDRESS_BRIEF = "0x1234...1234"
        const val TOKEN_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
    }

    // endregion
}