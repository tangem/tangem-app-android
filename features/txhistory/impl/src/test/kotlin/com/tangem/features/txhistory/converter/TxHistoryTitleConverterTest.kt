package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.models.network.TxInfo.TransactionStatus
import com.tangem.features.txhistory.impl.R
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryTitleConverterTest {

    private val coin = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)

    private val converter = TxHistoryTitleConverter(coin)

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: TitleModel) {
        // Act
        val actual = converter.convert(model.tx, isOwnTransfer = model.isOwnTransfer)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        // Pills resolve to the status-aware label text (no amount)
        TitleModel(
            tx = txInfo(TransactionType.Approve(amount = SdkAmount(currencySymbol = "USDT", value = null, decimals = 6), address = "0xspender")),
            expected = resRef(R.string.common_approved),
        ),
        TitleModel(
            tx = txInfo(
                type = TransactionType.Approve(
                    amount = SdkAmount(currencySymbol = "USDT", value = null, decimals = 6),
                    address = "0xspender",
                ),
                status = TransactionStatus.Failed,
            ),
            expected = resRef(R.string.transaction_history_status_approve_failed),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Stake),
            expected = resRef(R.string.common_staked),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Stake, status = TransactionStatus.Unconfirmed),
            expected = resRef(R.string.common_staking),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Stake, status = TransactionStatus.Failed),
            expected = resRef(R.string.transaction_history_status_stake_failed),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Unstake),
            expected = resRef(R.string.staking_unstaked),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Unstake, status = TransactionStatus.Unconfirmed),
            expected = resRef(R.string.staking_unstaking),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Unstake, status = TransactionStatus.Failed),
            expected = resRef(R.string.transaction_history_status_unstake_failed),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Restake),
            expected = resRef(R.string.transaction_history_rewards_restaked),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Restake, status = TransactionStatus.Unconfirmed),
            expected = resRef(R.string.transaction_history_status_restaking_rewards),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Restake, status = TransactionStatus.Failed),
            expected = resRef(R.string.transaction_history_status_rewards_restake_failed),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Vote(validatorAddress = "0xv"), status = TransactionStatus.Failed),
            expected = resRef(R.string.common_action_failed, listOf(resRef(R.string.staking_vote))),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.ClaimRewards, status = TransactionStatus.Unconfirmed),
            expected = resRef(R.string.transaction_history_status_claiming_rewards),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.ClaimRewards, status = TransactionStatus.Confirmed),
            expected = resRef(R.string.transaction_history_status_rewards_claimed),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.ClaimRewards, status = TransactionStatus.Failed),
            expected = resRef(R.string.transaction_history_status_rewards_claim_failed),
        ),
        TitleModel(
            tx = txInfo(TransactionType.YieldSupply.Enter(address = "0xa")),
            expected = resRef(R.string.yield_module_transaction_enter),
        ),
        TitleModel(
            tx = txInfo(TransactionType.YieldSupply.Exit(address = "0xa")),
            expected = resRef(R.string.yield_module_transaction_exit),
        ),
        // Content titles
        TitleModel(
            tx = txInfo(TransactionType.Swap, status = TransactionStatus.Confirmed),
            expected = resRef(R.string.common_swapped),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Swap, status = TransactionStatus.Unconfirmed),
            expected = resRef(R.string.common_swapping),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Swap, status = TransactionStatus.Failed),
            expected = resRef(R.string.transaction_history_status_swap_failed),
        ),
        // Transfer: own vs external, direction-aware
        TitleModel(
            tx = txInfo(TransactionType.Transfer, isOutgoing = true),
            isOwnTransfer = true,
            expected = resRef(R.string.common_transferred),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Transfer, isOutgoing = true, status = TransactionStatus.Unconfirmed),
            isOwnTransfer = true,
            expected = resRef(R.string.transaction_history_status_transferring),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Transfer, isOutgoing = true, status = TransactionStatus.Failed),
            isOwnTransfer = true,
            expected = resRef(R.string.transaction_history_status_transfer_failed),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Transfer, isOutgoing = true),
            expected = resRef(R.string.common_sent),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Transfer, isOutgoing = true, status = TransactionStatus.Failed),
            expected = resRef(R.string.transaction_history_status_send_failed),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Transfer, isOutgoing = false),
            expected = resRef(R.string.common_received),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Transfer, isOutgoing = false, status = TransactionStatus.Failed),
            expected = resRef(R.string.transaction_history_status_receive_failed),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Operation(name = "Mint")),
            expected = TextReference.Str("Mint"),
        ),
        TitleModel(
            tx = txInfo(TransactionType.YieldSupply.Topup),
            expected = resRef(R.string.transaction_history_supplied),
        ),
        TitleModel(
            tx = txInfo(TransactionType.YieldSupply.Topup, status = TransactionStatus.Unconfirmed),
            expected = resRef(R.string.common_supplying),
        ),
        TitleModel(
            tx = txInfo(TransactionType.YieldSupply.Topup, status = TransactionStatus.Failed),
            expected = resRef(R.string.transaction_history_status_supply_failed),
        ),
        TitleModel(
            tx = txInfo(
                type = TransactionType.YieldSupply.Send(address = "0xdest", isYieldSupplyWithdraw = true),
                status = TransactionStatus.Failed,
            ),
            expected = resRef(R.string.transaction_history_status_withdraw_failed),
        ),
        // A non-withdraw Send is a plain transfer, so it follows the same own-vs-external wording as Transfer
        TitleModel(
            tx = txInfo(
                type = TransactionType.YieldSupply.Send(address = "0xdest", isYieldSupplyWithdraw = false),
                isOutgoing = true,
            ),
            isOwnTransfer = true,
            expected = resRef(R.string.common_transferred),
        ),
        TitleModel(
            tx = txInfo(
                type = TransactionType.YieldSupply.Send(address = "0xdest", isYieldSupplyWithdraw = false),
                isOutgoing = false,
            ),
            isOwnTransfer = true,
            expected = resRef(R.string.common_transferred),
        ),
        TitleModel(
            tx = txInfo(
                type = TransactionType.YieldSupply.Send(address = "0xdest", isYieldSupplyWithdraw = false),
                isOutgoing = true,
                status = TransactionStatus.Unconfirmed,
            ),
            isOwnTransfer = true,
            expected = resRef(R.string.transaction_history_status_transferring),
        ),
        TitleModel(
            tx = txInfo(
                type = TransactionType.YieldSupply.Send(address = "0xdest", isYieldSupplyWithdraw = false),
                isOutgoing = true,
                status = TransactionStatus.Failed,
            ),
            isOwnTransfer = true,
            expected = resRef(R.string.transaction_history_status_transfer_failed),
        ),
        TitleModel(
            tx = txInfo(
                type = TransactionType.YieldSupply.Send(address = "0xdest", isYieldSupplyWithdraw = false),
                isOutgoing = true,
            ),
            expected = resRef(R.string.common_sent),
        ),
        TitleModel(
            tx = txInfo(TransactionType.UnknownOperation),
            expected = resRef(R.string.transaction_history_operation),
        ),
        TitleModel(
            tx = txInfo(TransactionType.GaslessFee),
            expected = resRef(R.string.common_network_fee_title),
        ),
        TitleModel(
            tx = txInfo(
                TransactionType.NetworkFee(
                    amount = null,
                    address = "0x3333333333333333333333333333333333333333",
                ),
            ),
            expected = resRef(R.string.common_network_fee_title),
        ),
        // An outgoing coin call that moved nothing is titled by the fee it cost, named method or not
        TitleModel(
            tx = txInfo(
                type = TransactionType.Operation(name = "YieldSend"),
                isOutgoing = true,
                amount = BigDecimal.ZERO,
            ),
            expected = resRef(R.string.common_network_fee_title),
        ),
        TitleModel(
            tx = txInfo(TransactionType.UnknownOperation, isOutgoing = true, amount = BigDecimal.ZERO),
            expected = resRef(R.string.common_network_fee_title),
        ),
        // The same call incoming was paid for by its sender, so it keeps the operation title
        TitleModel(
            tx = txInfo(TransactionType.Operation(name = "YieldSend"), amount = BigDecimal.ZERO),
            expected = TextReference.Str("YieldSend"),
        ),
    )

    internal data class TitleModel(
        val tx: TxInfo,
        val expected: TextReference,
        val isOwnTransfer: Boolean = false,
    )

    private fun txInfo(
        type: TransactionType,
        status: TransactionStatus = TransactionStatus.Confirmed,
        isOutgoing: Boolean = false,
        amount: BigDecimal = BigDecimal.ONE,
    ): TxInfo.Single = TxInfo.Single(
        txHash = "0xtxhash",
        timestampInMillis = 1_700_000_000_000L,
        isOutgoing = isOutgoing,
        destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User("0xdest")),
        sourceType = TxInfo.SourceType.Single(address = "0xsrc"),
        interactionAddressType = null,
        status = status,
        type = type,
        amount = amount,
    )

    private fun resRef(id: Int): TextReference = TextReference.Res(id = id)

    private fun resRef(id: Int, args: List<Any>): TextReference = TextReference.Res(
        id = id,
        formatArgs = WrappedList(args),
    )
}