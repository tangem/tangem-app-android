package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
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

    private val converter = TxHistoryTitleConverter()

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
            tx = txInfo(TransactionType.Approve),
            expected = resRef(R.string.common_approved),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Stake),
            expected = resRef(R.string.common_staked),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Unstake),
            expected = resRef(R.string.staking_unstaked),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Restake),
            expected = resRef(R.string.transaction_history_rewards_restaked),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Staking.Vote(validatorAddress = "0xv"), status = TransactionStatus.Failed),
            expected = resRef(R.string.common_action_failed, listOf(resRef(R.string.staking_vote))),
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
        // Transfer: own vs external, direction-aware
        TitleModel(
            tx = txInfo(TransactionType.Transfer, isOutgoing = true),
            isOwnTransfer = true,
            expected = resRef(R.string.common_transferred),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Transfer, isOutgoing = true),
            expected = resRef(R.string.common_sent),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Transfer, isOutgoing = false),
            expected = resRef(R.string.common_received),
        ),
        TitleModel(
            tx = txInfo(TransactionType.Operation(name = "Mint")),
            expected = TextReference.Str("Mint"),
        ),
        TitleModel(
            tx = txInfo(TransactionType.YieldSupply.Topup),
            expected = resRef(R.string.yield_module_transaction_topup),
        ),
        TitleModel(
            tx = txInfo(TransactionType.UnknownOperation),
            expected = resRef(R.string.transaction_history_operation),
        ),
        TitleModel(
            tx = txInfo(TransactionType.GaslessFee),
            expected = resRef(R.string.gasless_transaction_fee),
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
    ): TxInfo = TxInfo(
        txHash = "0xtxhash",
        timestampInMillis = 1_700_000_000_000L,
        isOutgoing = isOutgoing,
        destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User("0xdest")),
        sourceType = TxInfo.SourceType.Single(address = "0xsrc"),
        interactionAddressType = null,
        status = status,
        type = type,
        amount = BigDecimal.ONE,
    )

    private fun resRef(id: Int): TextReference = TextReference.Res(id = id)

    private fun resRef(id: Int, args: List<Any>): TextReference = TextReference.Res(
        id = id,
        formatArgs = WrappedList(args),
    )
}