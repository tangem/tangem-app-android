package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnChainTxToDetailsUMConverterTest : TxDetailsConverterTestBase() {

    private val converter = onChainConverter()

    // region Header

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN any TransactionType WHEN convert THEN SingleAsset produced`(type: TransactionType) {
        // Act
        val result = converter.convert(txInfo(type = type))

        // Assert
        assertThat(result).isInstanceOf(TxHistoryDetailsUM.SingleAsset::class.java)
    }

    private fun provideTestModels() = listOf(
        TransactionType.Transfer,
        TransactionType.Approve,
        TransactionType.Operation(name = "Mint NFT"),
        TransactionType.UnknownOperation,
        TransactionType.GaslessFee,
        TransactionType.Swap,
        TransactionType.Staking.Stake,
        TransactionType.Staking.ClaimRewards,
        TransactionType.Staking.Vote(validatorAddress = VALIDATOR_ADDRESS),
        TransactionType.YieldSupply.Topup,
        TransactionType.YieldSupply.Enter(address = USER_ADDRESS),
    )

    @Test
    fun `GIVEN incoming confirmed external Transfer WHEN convert THEN header has down icon, confirmed status, received title`() {
        // Arrange
        val tx = txInfo(
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
        val tx = txInfo(
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
        val ownConverter = onChainConverter(ownAddresses = setOf(USER_ADDRESS))
        val tx = txInfo(
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
        val ownConverter = onChainConverter(ownAddresses = setOf(USER_ADDRESS))
        val tx = txInfo(
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
        val tx = txInfo(type = TransactionType.Swap)

        // Act
        val header = converter.convert(tx).header

        // Assert
        assertThat(header.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20))
    }

    @Test
    fun `GIVEN a menu WHEN convert THEN header carries it verbatim`() {
        // Arrange — the converter is handed a ready menu; it must place it on the header untouched.
        val item = TxHistoryDetailsUM.MenuItemUM(
            icon = Icons.ic_arrow_down_20,
            title = resourceReference(R.string.common_share),
            onClick = {},
        )

        // Act
        val header = onChainConverter(menu = persistentListOf(item))
            .convert(txInfo(type = TransactionType.Transfer)).header

        // Assert
        assertThat(header.menu).containsExactly(item)
    }

    // endregion

    // region Amount block

    @Test
    fun `GIVEN incoming Transfer WHEN convert THEN amount block has plus sign and not failed`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Transfer, isOutgoing = false)

        // Act
        val amountBlock = converter.convert(tx).amountBlock

        // Assert
        assertThat(amountBlock.amount.resolveString()).startsWith("+ ")
        assertThat(amountBlock.isFailed).isFalse()
    }

    @Test
    fun `GIVEN outgoing Transfer WHEN convert THEN amount block has minus sign`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Transfer, isOutgoing = true)

        // Act
        val amountBlock = converter.convert(tx).amountBlock

        // Assert
        assertThat(amountBlock.amount.resolveString()).startsWith("- ")
    }

    @Test
    fun `GIVEN zero amount WHEN convert THEN amount block has no sign`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Transfer, isOutgoing = true, amount = BigDecimal.ZERO)

        // Act
        val amount = converter.convert(tx).amountBlock.amount.resolveString()

        // Assert
        assertThat(amount).doesNotContain("+")
        assertThat(amount).doesNotContain("-")
    }

    @Test
    fun `GIVEN failed outgoing Transfer WHEN convert THEN amount block is failed and drops the sign`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            status = TxInfo.TransactionStatus.Failed,
        )

        // Act
        val amountBlock = converter.convert(tx).amountBlock

        // Assert
        assertThat(amountBlock.isFailed).isTrue()
        val amount = amountBlock.amount.resolveString()
        assertThat(amount).doesNotContain("+")
        assertThat(amount).doesNotContain("-")
    }

    @ParameterizedTest
    @MethodSource("provideUnsignedAmountTypes")
    fun `GIVEN protocol-interaction tx WHEN convert THEN amount has no sign regardless of direction`(
        type: TransactionType,
    ) {
        // Arrange — staking (except ClaimRewards) and approvals show the operation amount, not a signed transfer.
        val tx = txInfo(type = type, isOutgoing = true)

        // Act
        val amount = converter.convert(tx).amountBlock.amount.resolveString()

        // Assert
        assertThat(amount).doesNotContain("+")
        assertThat(amount).doesNotContain("-")
    }

    private fun provideUnsignedAmountTypes() = listOf(
        TransactionType.Staking.Stake,
        TransactionType.Staking.Unstake,
        TransactionType.Staking.Restake,
        TransactionType.Staking.Withdraw,
        TransactionType.Staking.Vote(validatorAddress = VALIDATOR_ADDRESS),
        TransactionType.Approve,
    )

    @Test
    fun `GIVEN ClaimRewards WHEN convert THEN amount has plus sign regardless of direction`() {
        // Arrange — rewards are an inflow even when the chain reports the claiming tx as outgoing.
        val tx = txInfo(type = TransactionType.Staking.ClaimRewards, isOutgoing = true)

        // Act
        val amount = converter.convert(tx).amountBlock.amount.resolveString()

        // Assert
        assertThat(amount).startsWith("+ ")
    }

    @Test
    fun `GIVEN failed ClaimRewards WHEN convert THEN sign is dropped`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Staking.ClaimRewards, status = TxInfo.TransactionStatus.Failed)

        // Act
        val amountBlock = converter.convert(tx).amountBlock

        // Assert
        assertThat(amountBlock.isFailed).isTrue()
        assertThat(amountBlock.amount.resolveString()).doesNotContain("+")
    }

    @Test
    fun `GIVEN non-yield Transfer WHEN convert THEN single icon and no label`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Transfer)

        // Act
        val amountBlock = converter.convert(tx).amountBlock

        // Assert
        assertThat(amountBlock.icon).isInstanceOf(TxHistoryDetailsUM.AmountIconUM.Single::class.java)
        assertThat(amountBlock.label).isNull()
    }

    @Test
    fun `GIVEN yield-supply Enter WHEN convert THEN Supplied label, Aave-leading pair and unsigned amount`() {
        // Arrange — enter (funds outgoing to Aave): the amount must still be unsigned.
        val tx = txInfo(type = TransactionType.YieldSupply.Enter(address = USER_ADDRESS), isOutgoing = true)

        // Act
        val amountBlock = converter.convert(tx).amountBlock

        // Assert
        assertThat(amountBlock.label).isEqualTo(resourceReference(R.string.yield_module_transaction_supplied))
        val pair = amountBlock.icon as TxHistoryDetailsUM.AmountIconUM.OverlappingPair
        assertThat(pair.leading).isEqualTo(TxHistoryDetailsUM.AmountIconUM.Item.Resource(R.drawable.img_aave_22))
        assertThat(pair.trailing).isInstanceOf(TxHistoryDetailsUM.AmountIconUM.Item.Currency::class.java)
        val amount = amountBlock.amount.resolveString()
        assertThat(amount).doesNotContain("+")
        assertThat(amount).doesNotContain("-")
    }

    @Test
    fun `GIVEN yield-supply Exit WHEN convert THEN Returned label, asset-leading pair and unsigned amount`() {
        // Arrange
        val tx = txInfo(type = TransactionType.YieldSupply.Exit(address = USER_ADDRESS), isOutgoing = false)

        // Act
        val amountBlock = converter.convert(tx).amountBlock

        // Assert
        assertThat(amountBlock.label).isEqualTo(resourceReference(R.string.yield_module_transaction_returned))
        val pair = amountBlock.icon as TxHistoryDetailsUM.AmountIconUM.OverlappingPair
        assertThat(pair.leading).isInstanceOf(TxHistoryDetailsUM.AmountIconUM.Item.Currency::class.java)
        assertThat(pair.trailing).isEqualTo(TxHistoryDetailsUM.AmountIconUM.Item.Resource(R.drawable.img_aave_22))
        val amount = amountBlock.amount.resolveString()
        assertThat(amount).doesNotContain("+")
        assertThat(amount).doesNotContain("-")
    }

    // endregion

    // region Counterparty

    @Test
    fun `GIVEN no interaction address WHEN convert THEN counterparty is null`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Transfer, interactionAddressType = null)

        // Act
        val counterparty = converter.convert(tx).counterparty

        // Assert
        assertThat(counterparty).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideContractInteractionTypes")
    fun `GIVEN contract-interaction tx with User interaction address WHEN convert THEN no counterparty card`(
        type: TransactionType,
    ) {
        // Arrange — yield-supply / staking / approve talk to a protocol/validator, not a copyable recipient.
        val tx = txInfo(type = type, interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS))

        // Act
        val counterparty = converter.convert(tx).counterparty

        // Assert
        assertThat(counterparty).isNull()
    }

    private fun provideContractInteractionTypes() = listOf(
        TransactionType.YieldSupply.Enter(address = USER_ADDRESS),
        TransactionType.YieldSupply.Exit(address = USER_ADDRESS),
        TransactionType.Staking.Stake,
        TransactionType.Staking.Vote(validatorAddress = VALIDATOR_ADDRESS),
        TransactionType.Approve,
    )

    @Test
    fun `GIVEN incoming Transfer with User address WHEN convert THEN address-avatar counterparty with From label`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = converter.convert(tx).counterparty

        // Assert
        assertThat(counterparty?.avatar).isEqualTo(TxHistoryDetailsUM.CounterpartyAvatar.Address(USER_ADDRESS))
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.common_from))
    }

    @Test
    fun `GIVEN outgoing Transfer with User address WHEN convert THEN counterparty has Recipient label`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = converter.convert(tx).counterparty

        // Assert
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.send_recipient))
    }

    @Test
    fun `GIVEN address counterparty WHEN onCopyClick invoked THEN raw address is copied`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.Transfer,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )
        val counterparty = converter.convert(tx).counterparty

        // Act
        counterparty?.onCopyClick?.invoke()

        // Assert
        assertThat(copiedAddresses).containsExactly(USER_ADDRESS)
    }

    // endregion

    // region Network-fee row

    @Test
    fun `GIVEN tx with fee WHEN convert THEN single network-fee row`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.Transfer,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val rows = converter.convert(tx).rows

        // Assert
        assertThat(rows).hasSize(1)
        assertThat(rows.first().label).isEqualTo(resourceReference(R.string.common_network_fee_title))
        assertThat(rows.first().value.resolveString()).contains("ETH")
    }

    @Test
    fun `GIVEN tx without fee WHEN convert THEN no rows`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Transfer, fee = null)

        // Act
        val rows = converter.convert(tx).rows

        // Assert
        assertThat(rows).isEmpty()
    }

    // endregion

    // region Validator row

    @Test
    fun `GIVEN Vote tx with resolved validator having website WHEN convert THEN validator row links to the website`() {
        // Arrange — the Vote type carries the validator address; the yield resolves it to name + website.
        val tx = txInfo(type = TransactionType.Staking.Vote(validatorAddress = VALIDATOR_ADDRESS))

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        val validatorRow = rows.single()
        assertThat(validatorRow.label).isEqualTo(resourceReference(R.string.staking_validator))
        assertThat(validatorRow.value.resolveString()).isEqualTo("Lido Finance")
        assertThat(validatorRow.trailingIconRes).isEqualTo(R.drawable.ic_arrow_top_right_24)
        validatorRow.onClick?.invoke()
        assertThat(openedUrls).containsExactly(VALIDATOR_URL)
    }

    @Test
    fun `GIVEN Stake tx with validator interaction address WHEN convert THEN validator row resolved`() {
        // Arrange — non-Vote staking types surface the validator through the interaction address (e.g. Solana Stake).
        val tx = txInfo(
            type = TransactionType.Staking.Stake,
            interactionAddressType = TxInfo.InteractionAddressType.Validator(VALIDATOR_ADDRESS),
        )

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows.single().value.resolveString()).isEqualTo("Lido Finance")
    }

    @Test
    fun `GIVEN Stake tx with validator destination address WHEN convert THEN validator row resolved`() {
        // Arrange — the validator can also arrive as the destination address type.
        val tx = txInfo(
            type = TransactionType.Staking.Stake,
            destinationType = TxInfo.DestinationType.Single(
                addressType = TxInfo.AddressType.Validator(VALIDATOR_ADDRESS),
            ),
        )

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows.single().value.resolveString()).isEqualTo("Lido Finance")
    }

    @Test
    fun `GIVEN resolved validator without website WHEN convert THEN validator row has no link`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Staking.Vote(validatorAddress = VALIDATOR_ADDRESS))

        // Act
        val rows = onChainConverter(validators = listOf(validator(website = null))).convert(tx).rows

        // Assert
        val validatorRow = rows.single()
        assertThat(validatorRow.value.resolveString()).isEqualTo("Lido Finance")
        assertThat(validatorRow.trailingIconRes).isNull()
        assertThat(validatorRow.onClick).isNull()
    }

    @Test
    fun `GIVEN validator row and network fee WHEN convert THEN validator row precedes the fee row`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.Staking.Vote(validatorAddress = VALIDATOR_ADDRESS),
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows.map { it.label }).containsExactly(
            resourceReference(R.string.staking_validator),
            resourceReference(R.string.common_network_fee_title),
        ).inOrder()
    }

    @Test
    fun `GIVEN staking tx with address absent from yield WHEN convert THEN no validator row`() {
        // Arrange — the tx carries a validator address, but the current yield does not list it.
        val tx = txInfo(type = TransactionType.Staking.Vote(validatorAddress = "0xunknown"))

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows).isEmpty()
    }

    @Test
    fun `GIVEN staking tx with no validator address WHEN convert THEN no validator row`() {
        // Arrange — ClaimRewards carries no validator address at all.
        val tx = txInfo(type = TransactionType.Staking.ClaimRewards)

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows).isEmpty()
    }

    @Test
    fun `GIVEN non-staking tx with validators available WHEN convert THEN no validator row`() {
        // Arrange — a plain transfer is never a staking op, so the validator row is not offered.
        val tx = txInfo(
            type = TransactionType.Transfer,
            interactionAddressType = TxInfo.InteractionAddressType.Validator(VALIDATOR_ADDRESS),
        )

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows).isEmpty()
    }

    // endregion

    // region Protocol row (yield-supply)

    @Test
    fun `GIVEN yield-supply tx WHEN convert THEN protocol row shows the hard-wired Aave protocol with its link`() {
        // Arrange — yield-supply is a single hard-wired integration (Aave), so the value and link are constant.
        val tx = txInfo(type = TransactionType.YieldSupply.Enter(address = USER_ADDRESS))

        // Act
        val row = converter.convert(tx).rows.single()

        // Assert
        assertThat(row.label).isEqualTo(resourceReference(R.string.staking_validator))
        assertThat(row.value).isEqualTo(resourceReference(R.string.yield_module_provider))
        assertThat(row.trailingIconRes).isEqualTo(R.drawable.ic_arrow_top_right_24)
        row.onClick?.invoke()
        assertThat(openedUrls).containsExactly("https://aave.com/")
    }

    @Test
    fun `GIVEN yield-supply tx with network fee WHEN convert THEN protocol row precedes the fee row`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.YieldSupply.Topup,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val rows = converter.convert(tx).rows

        // Assert
        assertThat(rows.map { it.label }).containsExactly(
            resourceReference(R.string.staking_validator),
            resourceReference(R.string.common_network_fee_title),
        ).inOrder()
    }

    // endregion
}