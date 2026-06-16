package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.converter.TxHistoryStatusPillConverter.Input
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryStatusPillConverterTest {

    private val txHistoryUiActions: TxHistoryUiActions = mockk(relaxed = true)
    private val coin = createCoin(symbol = "ETH", decimals = 18)
    private val converter = TxHistoryStatusPillConverter(coin, txHistoryUiActions)

    // region Approve

    @Test
    fun `GIVEN Approve uiStatus Confirmed with User address WHEN convert THEN approved label and address subtitle`() {
        val tx = txInfo(
            type = TransactionType.Approve,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = converter.convert(Input(tx, Status.Confirmed, ApproveSpec))

        assertThat(result.kind).isEqualTo(TransactionItemUM.PillKind.APPROVE)
        assertThat(result.status).isEqualTo(Status.Confirmed)
        assertThat(result.label).isEqualTo(resRef(R.string.common_approved))
        assertThat(result.amount).isNotNull()
        assertThat(result.currencySymbol).isEqualTo("ETH")
        val subtitle = result.subtitle as TransactionItemUM.PillSubtitle.Address
        assertThat(subtitle.rawAddress).isEqualTo(USER_ADDRESS)
        assertThat(subtitle.briefAddress).isEqualTo(USER_ADDRESS_BRIEF)
    }

    @Test
    fun `GIVEN Approve uiStatus Unconfirmed WHEN convert THEN approving label`() {
        val tx = txInfo(
            type = TransactionType.Approve,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = converter.convert(Input(tx, Status.Unconfirmed, ApproveSpec))

        assertThat(result.label).isEqualTo(resRef(R.string.common_approving))
    }

    @Test
    fun `GIVEN Approve uiStatus Failed WHEN convert THEN non-composed approving label and no subtitle`() {
        val tx = txInfo(
            type = TransactionType.Approve,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        val result = converter.convert(Input(tx, Status.Failed, ApproveSpec))

        assertThat(result.label).isEqualTo(resRef(R.string.common_approving))
        assertThat(result.subtitle).isNull()
    }

    @Test
    fun `GIVEN Approve uiStatus Confirmed without User interaction address WHEN convert THEN no subtitle`() {
        val tx = txInfo(
            type = TransactionType.Approve,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
        )

        val result = converter.convert(Input(tx, Status.Confirmed, ApproveSpec))

        assertThat(result.subtitle).isNull()
    }

    // endregion

    // region Staking

    @Test
    fun `GIVEN Stake uiStatus Confirmed WHEN convert THEN staked label and amount`() {
        val tx = txInfo(type = TransactionType.Staking.Stake, amount = BigDecimal("1.5"))

        val result = converter.convert(Input(tx, Status.Confirmed, StakeSpec))

        assertThat(result.kind).isEqualTo(TransactionItemUM.PillKind.STAKING)
        assertThat(result.label).isEqualTo(resRef(R.string.common_staked))
        assertThat(result.amount).isNotNull()
        assertThat(result.currencySymbol).isEqualTo("ETH")
    }

    @Test
    fun `GIVEN Stake uiStatus Failed WHEN convert THEN composed failed label and no amount`() {
        val tx = txInfo(type = TransactionType.Staking.Stake)

        val result = converter.convert(Input(tx, Status.Failed, StakeSpec))

        assertThat(result.label).isEqualTo(
            resRef(R.string.common_action_failed, listOf(resRef(R.string.common_staking))),
        )
        assertThat(result.amount).isNull()
        assertThat(result.currencySymbol).isNull()
    }

    @Test
    fun `GIVEN Unstake uiStatus Confirmed WHEN convert THEN unstaked label`() {
        val tx = txInfo(type = TransactionType.Staking.Unstake)

        val result = converter.convert(Input(tx, Status.Confirmed, UnstakeSpec))

        assertThat(result.label).isEqualTo(resRef(R.string.staking_unstaked))
    }

    @Test
    fun `GIVEN Restake uiStatus Confirmed WHEN convert THEN restaked label`() {
        val tx = txInfo(type = TransactionType.Staking.Restake)

        val result = converter.convert(Input(tx, Status.Confirmed, RestakeSpec))

        assertThat(result.label).isEqualTo(resRef(R.string.transaction_history_rewards_restaked))
    }

    @Test
    fun `GIVEN Vote uiStatus Confirmed WHEN convert THEN vote label and no amount`() {
        val tx = txInfo(type = TransactionType.Staking.Vote(validatorAddress = "0xv"))

        val result = converter.convert(Input(tx, Status.Confirmed, VoteSpec))

        assertThat(result.label).isEqualTo(resRef(R.string.staking_vote))
        assertThat(result.amount).isNull()
    }

    @Test
    fun `GIVEN Vote uiStatus Failed WHEN convert THEN composed failed vote label`() {
        val tx = txInfo(type = TransactionType.Staking.Vote(validatorAddress = "0xv"))

        val result = converter.convert(Input(tx, Status.Failed, VoteSpec))

        assertThat(result.label).isEqualTo(
            resRef(R.string.common_action_failed, listOf(resRef(R.string.staking_vote))),
        )
    }

    @Test
    fun `GIVEN Withdraw uiStatus Confirmed WHEN convert THEN withdraw label and no amount`() {
        val tx = txInfo(type = TransactionType.Staking.Withdraw)

        val result = converter.convert(Input(tx, Status.Confirmed, WithdrawSpec))

        assertThat(result.label).isEqualTo(resRef(R.string.staking_withdraw))
        assertThat(result.amount).isNull()
    }

    // endregion

    // region YieldSupply

    @Test
    fun `GIVEN YieldEnter uiStatus Confirmed WHEN convert THEN enter label and no amount`() {
        val tx = txInfo(type = TransactionType.YieldSupply.Enter(address = USER_ADDRESS))

        val result = converter.convert(Input(tx, Status.Confirmed, YieldEnterSpec))

        assertThat(result.kind).isEqualTo(TransactionItemUM.PillKind.YIELD_MODE)
        assertThat(result.label).isEqualTo(resRef(R.string.yield_module_transaction_enter))
        assertThat(result.amount).isNull()
    }

    @Test
    fun `GIVEN YieldEnter uiStatus Failed WHEN convert THEN composed failed yield mode label`() {
        val tx = txInfo(type = TransactionType.YieldSupply.Enter(address = USER_ADDRESS))

        val result = converter.convert(Input(tx, Status.Failed, YieldEnterSpec))

        assertThat(result.label).isEqualTo(
            resRef(R.string.common_action_failed, listOf(resRef(R.string.common_yield_mode))),
        )
    }

    @Test
    fun `GIVEN YieldExit uiStatus Confirmed WHEN convert THEN exit label`() {
        val tx = txInfo(type = TransactionType.YieldSupply.Exit(address = USER_ADDRESS))

        val result = converter.convert(Input(tx, Status.Confirmed, YieldExitSpec))

        assertThat(result.label).isEqualTo(resRef(R.string.yield_module_transaction_exit))
    }

    @Test
    fun `GIVEN YieldExit uiStatus Failed WHEN convert THEN composed failed label`() {
        val tx = txInfo(type = TransactionType.YieldSupply.Exit(address = USER_ADDRESS))

        val result = converter.convert(Input(tx, Status.Failed, YieldExitSpec))

        assertThat(result.label).isEqualTo(
            resRef(
                R.string.common_action_failed,
                listOf(resRef(R.string.transaction_history_disabling_yield_mode)),
            ),
        )
    }

    // endregion

    // region Misc

    @Test
    fun `GIVEN any Pill WHEN onClick invoked THEN openTxInExplorer called with txHash`() {
        val tx = txInfo(type = TransactionType.Staking.Stake)

        val result = converter.convert(Input(tx, Status.Confirmed, StakeSpec))
        result.onClick()

        verify { txHistoryUiActions.openTxInExplorer(TX_HASH) }
    }

    @Test
    fun `GIVEN tx WHEN convert THEN txHash and timestamp propagated`() {
        val tx = txInfo(type = TransactionType.Staking.Stake)

        val result = converter.convert(Input(tx, Status.Confirmed, StakeSpec))

        assertThat(result.txHash).isEqualTo(TX_HASH)
        assertThat(result.timestamp).isEqualTo(TIMESTAMP)
    }

    // endregion

    // region Helpers

    private fun txInfo(
        type: TransactionType,
        amount: BigDecimal = BigDecimal.ONE,
        interactionAddressType: TxInfo.InteractionAddressType? = null,
    ): TxInfo = TxInfo(
        txHash = TX_HASH,
        timestampInMillis = TIMESTAMP,
        isOutgoing = false,
        destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(USER_ADDRESS)),
        sourceType = TxInfo.SourceType.Single(address = USER_ADDRESS),
        interactionAddressType = interactionAddressType,
        status = TxInfo.TransactionStatus.Confirmed,
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
        network = createNetwork(symbol = symbol),
        name = "Ethereum",
        symbol = symbol,
        decimals = decimals,
        iconUrl = null,
        isCustom = false,
    )

    private fun createNetwork(symbol: String): Network = Network(
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
    )

    private companion object {
        const val TX_HASH = "0xtxhash"
        const val TIMESTAMP = 1_700_000_000_000L
        const val USER_ADDRESS = "0x1234567890abcdef1234"
        const val USER_ADDRESS_BRIEF = "0x1234...1234"
    }

    // endregion
}