package com.tangem.features.txhistory.converter

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_document_20
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
        // Arrange — the counterparty resolves to one of the user's own accounts on the viewed network.
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
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
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
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
    fun `GIVEN own address reported in a different case WHEN convert THEN transferred title`() {
        // Arrange — a confirmed tx from an indexer may carry the own address in a different case (e.g. EIP-55
        // checksummed vs lowercase) than the locally-derived one the lookup was built from.
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS.uppercase() to ownAccount)),
        )
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
    fun `GIVEN own address with accounts mode off and no wallet info WHEN convert THEN sent title`() {
        // Arrange — mirrors the list: with accounts mode off and no wallet display info the owner stays external.
        val ownConverter = onChainConverter(
            lookup = lookupOf(
                currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount),
                isAccountsModeEnabled = false,
                walletInfoById = emptyMap(),
            ),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val header = ownConverter.convert(tx).header

        // Assert
        assertThat(header.title).isEqualTo(resourceReference(R.string.common_sent))
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
    fun `GIVEN outgoing tx with no interaction address WHEN convert THEN counterparty is null`() {
        // Arrange — outgoing, so the counterparty is the (absent) interaction/destination address, not the source.
        val tx = txInfo(type = TransactionType.Transfer, isOutgoing = true, interactionAddressType = null)

        // Act
        val counterparty = converter.convert(tx).counterparty

        // Assert
        assertThat(counterparty).isNull()
    }

    @Test
    fun `GIVEN incoming Swap whose source is own WHEN convert THEN no counterparty card`() {
        // Only an incoming unrecognized call reads the sender from source; every other type (Swap here) keeps its
        // interaction-address counterparty, which a swap does not carry, so no card is shown.
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = txInfo(
            type = TransactionType.Swap,
            isOutgoing = false,
            interactionAddressType = null,
            sourceType = TxInfo.SourceType.Single(address = USER_ADDRESS),
        )

        // Act
        val counterparty = ownConverter.convert(tx).counterparty

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
    fun `GIVEN incoming Transfer with User address WHEN convert THEN address-avatar counterparty with From address label`() {
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
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.common_from_address))
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
    fun `GIVEN Transfer to own Payment account WHEN convert THEN payment counterparty card without copy`() {
        // Arrange — the recipient address belongs to the user's own Tangem Pay (Payment) account.
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownPaymentAccount)),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = ownConverter.convert(tx).counterparty

        // Assert — resolved as own Payment account (not an external address): Visa avatar, name, no copy button.
        assertThat(counterparty?.avatar).isEqualTo(TxHistoryDetailsUM.CounterpartyAvatar.PaymentAccount)
        assertThat(counterparty?.onCopyClick).isNull()
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.send_recipient))
    }

    @Test
    fun `GIVEN incoming operation from external sender WHEN convert THEN counterparty is the sender not own destination`() {
        // Arrange — the trap: an incoming operation's interaction address is always the viewed (own) destination, so
        // resolving it would wrongly show "From <own account>". The counterparty must come from the real sender (source).
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint NFT"),
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
            sourceType = TxInfo.SourceType.Single(address = EXTERNAL_ADDRESS),
        )

        // Act
        val counterparty = ownConverter.convert(tx).counterparty

        // Assert
        assertThat(counterparty?.avatar).isEqualTo(TxHistoryDetailsUM.CounterpartyAvatar.Address(EXTERNAL_ADDRESS))
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.common_from_address))
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

    @Test
    fun `GIVEN Transfer to own account WHEN convert THEN account counterparty card without copy`() {
        // Arrange — the counterparty resolves to the user's own account, so the card shows its name and avatar.
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = ownConverter.convert(tx).counterparty

        // Assert
        assertThat(counterparty).isEqualTo(
            TxHistoryDetailsUM.CounterpartyUM(
                label = resourceReference(R.string.send_recipient),
                title = stringReference("Family"),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Account(
                    iconResId = ownAccount.icon.value.getResId(),
                    backgroundColor = ownAccount.icon.color.getUiColor(),
                ),
                onCopyClick = null,
            ),
        )
    }

    @Test
    fun `GIVEN Transfer to own address with accounts mode off WHEN convert THEN wallet counterparty card`() {
        // Arrange — with accounts mode off the owner renders as the owning wallet instead of the account.
        val ownConverter = onChainConverter(
            lookup = lookupOf(
                currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount),
                isAccountsModeEnabled = false,
            ),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = ownConverter.convert(tx).counterparty

        // Assert
        assertThat(counterparty).isEqualTo(
            TxHistoryDetailsUM.CounterpartyUM(
                label = resourceReference(R.string.send_recipient),
                title = stringReference("My Wallet"),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Wallet(
                    deviceIconUM = DeviceIconUM.Card(mainColor = Color(0xFF1E1E1E), secondColor = null),
                ),
                onCopyClick = null,
            ),
        )
    }

    @Test
    fun `GIVEN Transfer to address owned only on another network WHEN convert THEN external address card`() {
        // Arrange — the address is owned on bitcoin, not the viewed ethereum currency, so it stays external here.
        val ownConverter = onChainConverter(
            lookup = lookupOf(bitcoin.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = ownConverter.convert(tx).counterparty

        // Assert
        assertThat(counterparty?.avatar).isEqualTo(TxHistoryDetailsUM.CounterpartyAvatar.Address(USER_ADDRESS))
        assertThat(counterparty?.onCopyClick).isNotNull()
    }

    // endregion

    // region Operation / UnknownOperation reclassified as own transfer

    @Test
    fun `GIVEN incoming Operation from own address WHEN convert THEN transferred header and own account counterparty`() {
        // Arrange — an unrecognized call whose sender resolves to the user's own account reads as a transfer.
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = txInfo(type = TransactionType.Operation(name = "Mint NFT"), isOutgoing = false)

        // Act
        val result = ownConverter.convert(tx)

        // Assert
        assertThat(result.header.title).isEqualTo(resourceReference(R.string.common_transferred))
        assertThat(result.header.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_down_20))
        assertThat(result.counterparty).isEqualTo(
            TxHistoryDetailsUM.CounterpartyUM(
                label = resourceReference(R.string.common_from_account),
                title = stringReference("Family"),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Account(
                    iconResId = ownAccount.icon.value.getResId(),
                    backgroundColor = ownAccount.icon.color.getUiColor(),
                ),
                onCopyClick = null,
            ),
        )
    }

    @Test
    fun `GIVEN outgoing Operation whose recipient is external WHEN convert THEN stays Operation`() {
        // The trap: for an outgoing tx the counterparty is the recipient; an own sender must not trigger reclassification.
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint NFT"),
            isOutgoing = true,
            destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(EXTERNAL_ADDRESS)),
        )

        // Act
        val header = ownConverter.convert(tx).header

        // Assert
        assertThat(header.title).isEqualTo(stringReference("Mint NFT"))
        assertThat(header.icon).isEqualTo(TxIcon.Vector(Icons.ic_document_20))
    }

    // endregion

    // region Network-fee row

    @Test
    fun `GIVEN outgoing Transfer with fee WHEN convert THEN single network-fee row`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
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
    fun `GIVEN incoming Transfer with fee WHEN convert THEN no network-fee row`() {
        // A received transfer's fee belongs to the sender, not the user — it must not be shown.
        // Arrange
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = false,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val rows = converter.convert(tx).rows

        // Assert
        assertThat(rows).isEmpty()
    }

    @Test
    fun `GIVEN incoming ClaimRewards with fee WHEN convert THEN network-fee row shown`() {
        // Staking claims are isOutgoing = false yet the user paid the gas — the fee stays (only plain Transfers hide it).
        // Arrange
        val tx = txInfo(
            type = TransactionType.Staking.ClaimRewards,
            isOutgoing = false,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val rows = converter.convert(tx).rows

        // Assert
        assertThat(rows.map { it.label }).contains(resourceReference(R.string.common_network_fee_title))
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