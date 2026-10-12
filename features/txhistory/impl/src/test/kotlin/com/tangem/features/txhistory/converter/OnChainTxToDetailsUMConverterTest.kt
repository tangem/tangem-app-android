package com.tangem.features.txhistory.converter

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_chart_line_vertical_20
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.core.ui.res.generated.icons.ic_globe_24
import com.tangem.core.ui.res.generated.icons.ic_lightning_20
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.SdkAmountType
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.TestContacts
import com.tangem.test.mock.MockAccounts
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
        TransactionType.Approve(amount = SdkAmount(currencySymbol = "USDT", value = null, decimals = 6), address = USER_ADDRESS),
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
            action = TxHistoryDetailsUM.MenuItemUM.Action.Direct {},
        )

        // Act — a tx with no counterparty card, so no "Copy address" row is prepended to the given menu.
        val header = onChainConverter(menu = persistentListOf(item))
            .convert(txInfo(type = TransactionType.Transfer)).header

        // Assert
        assertThat(header.menu).containsExactly(item)
    }

    @Test
    fun `GIVEN external counterparty WHEN convert THEN Copy address leads the header menu`() {
        // Arrange — the counterparty is none of the user's portfolios, so its address is copyable.
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val menu = converter.convert(tx).header.menu

        // Assert
        assertThat(menu).hasSize(1)
        assertThat(menu[0].icon).isEqualTo(Icons.ic_copy_24)
        assertThat(menu[0].title).isEqualTo(resourceReference(R.string.common_copy_address))

        (menu[0].action as TxHistoryDetailsUM.MenuItemUM.Action.Direct).onClick()
        assertThat(copiedAddresses).containsExactly(USER_ADDRESS)
    }

    @Test
    fun `GIVEN own account counterparty WHEN convert THEN Copy address is still offered`() {
        // Arrange — the card reads "to <my account>", but the menu row copies that account's address all the same.
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val menu = ownConverter.convert(tx).header.menu

        // Assert
        assertThat(menu).hasSize(1)

        (menu[0].action as TxHistoryDetailsUM.MenuItemUM.Action.Direct).onClick()
        assertThat(copiedAddresses).containsExactly(USER_ADDRESS)
    }

    @Test
    fun `GIVEN staking tx WHEN convert THEN neither counterparty card nor Copy address row`() {
        // Arrange — a protocol interaction has no counterparty, so both entry points are dropped together.
        val tx = txInfo(
            type = TransactionType.Staking.Stake,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.Validator(VALIDATOR_ADDRESS),
        )

        // Act
        val details = converter.convert(tx)

        // Assert
        assertThat(details.counterparty).isNull()
        assertThat(details.header.menu).isEmpty()
    }

    @Test
    fun `GIVEN no counterparty address WHEN convert THEN neither counterparty card nor Copy address row`() {
        // Arrange — an outgoing tx that reports no interaction address names no counterparty at all.
        val tx = txInfo(type = TransactionType.Transfer, isOutgoing = true)

        // Act
        val details = converter.convert(tx)

        // Assert
        assertThat(details.counterparty).isNull()
        assertThat(details.header.menu).isEmpty()
    }

    @Test
    fun `GIVEN GaslessFee WHEN convert THEN neither counterparty card nor Copy address row`() {
        // Arrange — the fee collector is not a recipient the user chose, so the card names nobody
        val tx = txInfo(
            type = TransactionType.GaslessFee,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val details = converter.convert(tx)

        // Assert
        assertThat(details.counterparty).isNull()
        assertThat(details.header.menu).isEmpty()
    }

    @Test
    fun `GIVEN NetworkFee WHEN convert THEN counterparty names the asset the fee was paid for`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.NetworkFee(
                amount = SdkAmount(
                    currencySymbol = "USDT",
                    value = BigDecimal("5"),
                    decimals = 6,
                    type = SdkAmountType.Token(contractAddress = USER_ADDRESS),
                ),
                address = USER_ADDRESS,
            ),
            amount = BigDecimal.ZERO,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val details = converter.convert(tx)

        // Assert
        assertThat(details.counterparty?.label).isEqualTo(resourceReference(R.string.common_send))
        assertThat(details.counterparty?.avatar).isEqualTo(
            TxHistoryDetailsUM.CounterpartyAvatar.Glyph(icon = TxIcon.Vector(Icons.ic_arrow_up_20)),
        )
        assertThat(details.counterparty?.title).isEqualTo(TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference("5.00\u00A0USDT")))
        assertThat(details.header.menu).isEmpty()
    }

    @Test
    fun `GIVEN NetworkFee for an untracked token WHEN convert THEN no counterparty card`() {
        // Arrange — the asset cannot be named, and the recipient must not stand in for it
        val tx = txInfo(
            type = TransactionType.NetworkFee(amount = null, address = USER_ADDRESS),
            amount = BigDecimal.ZERO,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val details = converter.convert(tx)

        // Assert
        assertThat(details.counterparty).isNull()
        assertThat(details.header.menu).isEmpty()
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
        TransactionType.Approve(
            amount = SdkAmount(currencySymbol = "ETH", value = BigDecimal("1000"), decimals = 18),
            address = USER_ADDRESS,
        ),
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

    // region Approve amount / risk banner

    @Test
    fun `GIVEN Approve with a known allowance WHEN convert THEN amount block shows the allowance and no risk banner`() {
        // Arrange — the allowance amount comes from TransactionType.Approve, not the tx's own amount field.
        val tx = txInfo(
            type = TransactionType.Approve(
                amount = SdkAmount(currencySymbol = "USDT", value = BigDecimal("1000"), decimals = 6),
                address = USER_ADDRESS,
            ),
            amount = BigDecimal.ZERO,
        )

        // Act
        val result = converter.convert(tx)

        // Assert
        assertThat(result.amountBlock.amount.resolveString()).contains("USDT")
        assertThat(result.amountBlock.label).isEqualTo(resourceReference(R.string.transaction_history_approved_amount))
        assertThat(result.statusBanner).isNull()
    }

    @Test
    fun `GIVEN Approve with no allowance limit WHEN convert THEN amount block shows Unlimited and a Warning risk banner`() {
        // Arrange — a null allowance value means the approval is unlimited; the symbol is the approved token's, not the
        // viewed currency's.
        val tx = txInfo(type = TransactionType.Approve(amount = SdkAmount(currencySymbol = "USDT", value = null, decimals = 6), address = USER_ADDRESS))

        // Act
        val result = converter.convert(tx)

        // Assert
        assertThat(result.amountBlock.amount).isEqualTo(
            resourceReference(R.string.transaction_history_unlimited_amount, wrappedList("USDT")),
        )
        assertThat(result.statusBanner).isEqualTo(
            TxHistoryDetailsUM.StatusBannerUM(
                style = TxHistoryDetailsUM.StatusBannerUM.Style.Warning,
                title = resourceReference(R.string.transaction_history_approve_high_risk_title),
                subtitle = resourceReference(R.string.transaction_history_approve_high_risk_subtitle),
                isLoading = false,
            ),
        )
    }

    @Test
    fun `GIVEN Approve of a tracked token WHEN convert THEN amount icon is the approved token, not the viewed coin`() {
        // Arrange — an approval in the coin's history: the allowance is granted in USDC, the open history is Ethereum's.
        val usdc = mockCurrencyFactory.createToken(
            blockchain = Blockchain.Ethereum,
            contractAddress = USDC_CONTRACT,
            symbol = "USDC",
            decimals = 6,
        )
        val tx = txInfo(
            type = TransactionType.Approve(
                amount = SdkAmount(
                    currencySymbol = "USDC",
                    value = null,
                    decimals = 6,
                    type = SdkAmountType.Token(contractAddress = USDC_CONTRACT),
                ),
                address = USER_ADDRESS,
            ),
        )

        // Act
        val icon = onChainConverter(networkTokens = setOf(usdc)).convert(tx).amountBlock.icon

        // Assert
        assertThat(icon).isEqualTo(
            TxHistoryDetailsUM.AmountIconUM.Single(CryptoCurrencyToIconStateConverter().convert(usdc)),
        )
    }

    @Test
    fun `GIVEN Approve of an untracked token WHEN convert THEN amount icon falls back to the viewed currency`() {
        // Arrange — the wallet does not track the approved token, so its contract matches nothing.
        val tx = txInfo(
            type = TransactionType.Approve(
                amount = SdkAmount(
                    currencySymbol = "USDC",
                    value = null,
                    decimals = 6,
                    type = SdkAmountType.Token(contractAddress = USDC_CONTRACT),
                ),
                address = USER_ADDRESS,
            ),
        )

        // Act
        val icon = converter.convert(tx).amountBlock.icon

        // Assert
        assertThat(icon).isEqualTo(
            TxHistoryDetailsUM.AmountIconUM.Single(CryptoCurrencyToIconStateConverter().convert(currency)),
        )
    }

    @Test
    fun `GIVEN Approve with no allowance limit but Failed WHEN convert THEN no risk banner`() {
        // Arrange — an approval that never went through granted no real allowance, so there is nothing to warn about.
        val tx = txInfo(
            type = TransactionType.Approve(amount = SdkAmount(currencySymbol = "USDT", value = null, decimals = 6), address = USER_ADDRESS),
            status = TxInfo.TransactionStatus.Failed,
        )

        // Act
        val result = converter.convert(tx)

        // Assert
        assertThat(result.statusBanner).isNull()
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
        TransactionType.Approve(amount = SdkAmount(currencySymbol = "USDT", value = null, decimals = 6), address = USER_ADDRESS),
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

    private fun savedContact() = TestContacts.create(
        name = "Binance",
        address = USER_ADDRESS,
        walletId = MockAccounts.userWalletId,
        networkId = currency.network.id.rawId,
    )

    @Test
    fun `GIVEN outgoing Transfer to a saved contact WHEN convert THEN contact card named after it`() {
        // Arrange
        val contactConverter = onChainConverter(lookup = lookupOf(contacts = listOf(savedContact())))
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = contactConverter.convert(tx).counterparty

        // Assert — the name replaces the address, but the underlying address stays copyable.
        assertThat(counterparty?.title).isEqualTo(TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference("Binance")))
        assertThat(counterparty?.avatar).isEqualTo(
            TxHistoryDetailsUM.CounterpartyAvatar.Contact(
                name = "Binance",
                icon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Letter,
                    color = CryptoPortfolioIcon.Color.Azure,
                ),
            ),
        )
        assertThat(counterparty?.onCopyClick).isNotNull()
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.send_recipient))
    }

    @Test
    fun `GIVEN incoming Transfer from a saved contact WHEN convert THEN contact card named after it`() {
        // Arrange
        val contactConverter = onChainConverter(lookup = lookupOf(contacts = listOf(savedContact())))
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = contactConverter.convert(tx).counterparty

        // Assert — the sender is named after the contact, same as a recipient.
        assertThat(counterparty?.title).isEqualTo(TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference("Binance")))
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.common_from_address))
    }

    @Test
    fun `GIVEN outgoing Operation to a saved address WHEN convert THEN contact card named after it`() {
        // Arrange
        val contactConverter = onChainConverter(lookup = lookupOf(contacts = listOf(savedContact())))
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint NFT"),
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = contactConverter.convert(tx).counterparty

        // Assert — the address book is consulted whatever the transaction type (as in the history list).
        assertThat(counterparty?.title).isEqualTo(TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference("Binance")))
    }

    @Test
    fun `GIVEN outgoing Transfer to own address also saved as a contact WHEN convert THEN own account card`() {
        // Arrange
        val ownConverter = onChainConverter(
            lookup = lookupOf(
                currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount),
                contacts = listOf(savedContact()),
            ),
        )
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val counterparty = ownConverter.convert(tx).counterparty

        // Assert — an own portfolio outranks the address book.
        assertThat(counterparty?.avatar).isInstanceOf(TxHistoryDetailsUM.CounterpartyAvatar.Account::class.java)

        counterparty?.onCopyClick?.invoke()
        assertThat(copiedAddresses).containsExactly(USER_ADDRESS)
    }

    @Test
    fun `GIVEN Transfer to own Payment account WHEN convert THEN payment counterparty card copies its address`() {
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

        // Assert — resolved as own Payment account (not an external address): Visa avatar and name, address still copied.
        assertThat(counterparty?.avatar).isEqualTo(TxHistoryDetailsUM.CounterpartyAvatar.PaymentAccount)
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.send_recipient))

        counterparty?.onCopyClick?.invoke()
        assertThat(copiedAddresses).containsExactly(USER_ADDRESS)
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
    fun `GIVEN Transfer to own account WHEN convert THEN account counterparty card copies its address`() {
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
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.send_recipient))
        assertThat(counterparty?.title).isEqualTo(TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference("Family")))
        assertThat(counterparty?.avatar).isEqualTo(
            TxHistoryDetailsUM.CounterpartyAvatar.Account(
                name = stringReference("Family"),
                icon = CryptoPortfolioIconConverter.convert(ownAccount.icon),
            ),
        )

        counterparty?.onCopyClick?.invoke()
        assertThat(copiedAddresses).containsExactly(USER_ADDRESS)
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
        assertThat(counterparty?.label).isEqualTo(resourceReference(R.string.send_recipient))
        assertThat(counterparty?.title).isEqualTo(TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference("My Wallet")))
        assertThat(counterparty?.avatar).isEqualTo(
            TxHistoryDetailsUM.CounterpartyAvatar.Wallet(
                deviceIconUM = DeviceIconUM.Card(mainColor = Color(0xFF1E1E1E), secondColor = null),
            ),
        )

        counterparty?.onCopyClick?.invoke()
        assertThat(copiedAddresses).containsExactly(USER_ADDRESS)
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

        counterparty?.onCopyClick?.invoke()
        assertThat(copiedAddresses).containsExactly(USER_ADDRESS)
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
        assertThat(result.counterparty?.label).isEqualTo(resourceReference(R.string.common_from_account))
        assertThat(result.counterparty?.title).isEqualTo(TxHistoryDetailsUM.CounterpartyTitle.Name(stringReference("Family")))
        assertThat(result.counterparty?.avatar).isEqualTo(
            TxHistoryDetailsUM.CounterpartyAvatar.Account(
                name = stringReference("Family"),
                icon = CryptoPortfolioIconConverter.convert(ownAccount.icon),
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
        assertThat(rows.first().isValueHideable).isTrue()
    }

    @Test
    fun `GIVEN outgoing Transfer paid for in another token WHEN convert THEN fee row names that token`() {
        // A gaslessly sent transfer: the relayer paid the native gas, the wallet paid the service in USDT
        // Arrange
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            fee = SdkAmount(
                currencySymbol = "USDT",
                value = BigDecimal("0.065869"),
                decimals = 6,
                type = SdkAmountType.Token(contractAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7"),
            ),
        )

        // Act
        val rows = converter.convert(tx).rows

        // Assert
        assertThat(rows.single().label).isEqualTo(resourceReference(R.string.common_network_fee_title))
        assertThat(rows.single().value.resolveString()).isEqualTo("0.065869\u00A0USDT")
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
    fun `GIVEN incoming Operation with fee WHEN convert THEN network-fee row shown`() {
        // A contract call is reported incoming whenever it pays out to the user (an EVM token leg's direction is the
        // transfer's, not the signer's), yet the user signed and paid it — the fee stays. Only plain transfers hide it.
        // Arrange
        val tx = txInfo(
            type = TransactionType.Operation(name = "Mint NFT"),
            isOutgoing = false,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val rows = converter.convert(tx).rows

        // Assert
        assertThat(rows.map { it.label }).containsExactly(resourceReference(R.string.common_network_fee_title))
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

    @Test
    fun `GIVEN tx with zero fee WHEN convert THEN no rows`() {
        // Arrange — a gaslessly sent transfer pays its native fee through the relayer, leaving a zero fee here.
        val tx = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            fee = SdkAmount(currencySymbol = "TRX", value = BigDecimal.ZERO, decimals = 6),
        )

        // Act
        val rows = converter.convert(tx).rows

        // Assert
        assertThat(rows).isEmpty()
    }

    // endregion

    // region Validator row

    @Test
    fun `GIVEN Vote tx with resolved validator having website WHEN convert THEN validator row is plain text`() {
        // Arrange — the Vote type carries the validator address; the yield resolves it to name + website.
        val tx = txInfo(type = TransactionType.Staking.Vote(validatorAddress = VALIDATOR_ADDRESS))

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert — the website is never surfaced: no trailing link icon, no tap.
        assertThat(rows.single()).isEqualTo(
            TxHistoryDetailsUM.InfoRowUM(
                label = resourceReference(R.string.staking_validator),
                value = stringReference("Lido Finance"),
            ),
        )
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
    fun `GIVEN EVM staking tx whose contract interaction address is a known validator WHEN convert THEN row resolved`() {
        // Arrange — on EVM the staking call's destination is the validator contract, but the SDK types it as
        // Contract (only Solana ever emits AddressType.Validator), so a Contract address must still resolve.
        val tx = txInfo(
            type = TransactionType.Staking.Stake,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(VALIDATOR_ADDRESS),
        )

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows.single().value.resolveString()).isEqualTo("Lido Finance")
    }

    @Test
    fun `GIVEN staking tx whose address differs in case from the known validator WHEN convert THEN row resolved`() {
        // Arrange — an EVM address arrives checksummed or lowercased depending on the history provider, while the
        // staking API picks its own casing.
        val tx = txInfo(
            type = TransactionType.Staking.Stake,
            interactionAddressType = TxInfo.InteractionAddressType.Contract("0xVALIDATOR"),
        )

        // Act
        val rows = onChainConverter(validators = listOf(validator(address = "0xvalidator"))).convert(tx).rows

        // Assert
        assertThat(rows.single().value.resolveString()).isEqualTo("Lido Finance")
    }

    @Test
    fun `GIVEN Vote tx with blank validator address WHEN convert THEN no validator row`() {
        // Arrange — the Tron vote parser reports a missing validator as an empty string.
        val tx = txInfo(type = TransactionType.Staking.Vote(validatorAddress = ""))

        // Act
        val rows = onChainConverter(validators = listOf(validator(address = ""))).convert(tx).rows

        // Assert
        assertThat(rows).isEmpty()
    }

    @Test
    fun `GIVEN staking tx to a known P2P vault WHEN convert THEN row shows the vault name`() {
        // Arrange — a P2P pooled-staking deposit targets a vault contract, resolved from the same target map.
        val tx = txInfo(
            type = TransactionType.Staking.Stake,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(VAULT_ADDRESS),
        )

        // Act
        val rows = onChainConverter(vaults = listOf(vault())).convert(tx).rows

        // Assert
        val row = rows.single()
        assertThat(row.value.resolveString()).isEqualTo("P2P Vault")
        // The vaults API carries no per-vault page, so there is no link to offer.
        assertThat(row.trailingIconRes).isNull()
        assertThat(row.onClick).isNull()
    }

    @Test
    fun `GIVEN unrecognized contract call to a known vault WHEN convert THEN row shows the vault name`() {
        // Arrange — a staking call whose 4-byte selector is not registered in contract_methods.json (e.g. the P2P
        // exit-queue withdrawal) arrives as UnknownOperation, but its address still identifies the vault.
        val tx = txInfo(
            type = TransactionType.UnknownOperation,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(VAULT_ADDRESS),
        )

        // Act
        val rows = onChainConverter(vaults = listOf(vault())).convert(tx).rows

        // Assert
        assertThat(rows.single().value.resolveString()).isEqualTo("P2P Vault")
    }

    @Test
    fun `GIVEN plain transfer to a known validator address WHEN convert THEN no validator row`() {
        // Arrange — a value transfer is never a staking interaction, even when the counterparty is a known target.
        val tx = txInfo(
            type = TransactionType.Transfer,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(VALIDATOR_ADDRESS),
        )

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows).isEmpty()
    }

    @Test
    fun `GIVEN Vote tx with address absent from yield WHEN convert THEN row shows the raw address`() {
        // Arrange — the tx carries a validator address, but the current yield does not list it.
        val tx = txInfo(type = TransactionType.Staking.Vote(validatorAddress = UNKNOWN_VALIDATOR_ADDRESS))

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows.single()).isEqualTo(
            TxHistoryDetailsUM.InfoRowUM(
                label = resourceReference(R.string.staking_validator),
                value = stringReference(UNKNOWN_VALIDATOR_ADDRESS),
                isValueAddress = true,
            ),
        )
    }

    @Test
    fun `GIVEN Stake tx with validator address absent from yield WHEN convert THEN row shows the raw address`() {
        // Arrange — a validator dropped from the StakeKit yield must not make the row of an old tx disappear.
        val tx = txInfo(
            type = TransactionType.Staking.Stake,
            interactionAddressType = TxInfo.InteractionAddressType.Validator(UNKNOWN_VALIDATOR_ADDRESS),
        )

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows.single().value.resolveString()).isEqualTo(UNKNOWN_VALIDATOR_ADDRESS)
    }

    @Test
    fun `GIVEN staking tx with validator destination absent from yield WHEN convert THEN raw address shown`() {
        // Arrange — the validator can also arrive as the destination address type.
        val tx = txInfo(
            type = TransactionType.Staking.Stake,
            destinationType = TxInfo.DestinationType.Single(
                addressType = TxInfo.AddressType.Validator(UNKNOWN_VALIDATOR_ADDRESS),
            ),
        )

        // Act
        val rows = onChainConverter(validators = listOf(validator())).convert(tx).rows

        // Assert
        assertThat(rows.single().value.resolveString()).isEqualTo(UNKNOWN_VALIDATOR_ADDRESS)
    }

    @Test
    fun `GIVEN staking tx with unknown contract address WHEN convert THEN no validator row`() {
        // Arrange — a Contract address is a target only when the lookup confirms it; falling back to it would label
        // an arbitrary counterparty "Validator".
        val tx = txInfo(
            type = TransactionType.Staking.Stake,
            interactionAddressType = TxInfo.InteractionAddressType.Contract(UNKNOWN_VALIDATOR_ADDRESS),
        )

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

    // region Yield-supply rows

    @ParameterizedTest
    @MethodSource("provideYieldSupplyTypes")
    fun `GIVEN yield-supply tx WHEN convert THEN no validator row`(type: TransactionType.YieldSupply) {
        // Act
        val rows = converter.convert(txInfo(type = type)).rows

        // Assert — the Validator field belongs to staking; a yield-supply card names no protocol at all.
        assertThat(rows.map { it.label }).doesNotContain(resourceReference(R.string.staking_validator))
    }

    private fun provideYieldSupplyTypes() = listOf(
        TransactionType.YieldSupply.Enter(address = USER_ADDRESS),
        TransactionType.YieldSupply.Exit(address = USER_ADDRESS),
        TransactionType.YieldSupply.Topup,
        TransactionType.YieldSupply.DeployContract(address = USER_ADDRESS),
        TransactionType.YieldSupply.InitializeToken(address = USER_ADDRESS),
        TransactionType.YieldSupply.ReactivateToken(address = USER_ADDRESS),
        TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = true),
    )

    @ParameterizedTest
    @MethodSource("provideYieldSupplyTypes")
    fun `GIVEN yield-supply tx WHEN convert THEN Explore Aave closes the header menu`(
        type: TransactionType.YieldSupply,
    ) {
        // Act
        val menu = converter.convert(txInfo(type = type)).header.menu

        // Assert
        val exploreAave = menu.last()
        assertThat(exploreAave.icon).isEqualTo(Icons.ic_globe_24)
        assertThat(exploreAave.title)
            .isEqualTo(resourceReference(R.string.yield_module_explore_sheet_explore_aave_button_title))
        (exploreAave.action as TxHistoryDetailsUM.MenuItemUM.Action.Direct).onClick()
        assertThat(openedUrls).containsExactly("https://aave.com/")
    }

    @ParameterizedTest
    @MethodSource("provideYieldModeTypes")
    fun `GIVEN yield mode enabled or disabled WHEN convert THEN header shows the bars glyph`(
        type: TransactionType.YieldSupply,
    ) {
        // Act
        val icon = converter.convert(txInfo(type = type)).header.icon

        // Assert
        assertThat(icon).isEqualTo(TxIcon.Vector(Icons.ic_chart_line_vertical_20))
    }

    private fun provideYieldModeTypes() = listOf(
        TransactionType.YieldSupply.Enter(address = USER_ADDRESS),
        TransactionType.YieldSupply.Exit(address = USER_ADDRESS),
    )

    @Test
    fun `GIVEN yield-supply topup WHEN convert THEN header keeps the lightning glyph`() {
        // Act
        val icon = converter.convert(txInfo(type = TransactionType.YieldSupply.Topup)).header.icon

        // Assert
        assertThat(icon).isEqualTo(TxIcon.Vector(Icons.ic_lightning_20))
    }

    @Test
    fun `GIVEN yield-supply topup WHEN convert THEN asset-leading Aave pair and no label`() {
        // Act
        val amountBlock = converter.convert(txInfo(type = TransactionType.YieldSupply.Topup)).amountBlock

        // Assert — a topup mirrors a withdraw: the pair without the "Supplied"/"Returned" caption of enter/exit.
        val pair = amountBlock.icon as TxHistoryDetailsUM.AmountIconUM.OverlappingPair
        assertThat(pair.leading).isInstanceOf(TxHistoryDetailsUM.AmountIconUM.Item.Currency::class.java)
        assertThat(pair.trailing).isEqualTo(TxHistoryDetailsUM.AmountIconUM.Item.Resource(R.drawable.img_aave_22))
        assertThat(amountBlock.label).isNull()
    }

    @Test
    fun `GIVEN non yield-supply tx WHEN convert THEN no Explore Aave row`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Transfer)

        // Act
        val menu = converter.convert(tx).header.menu

        // Assert
        assertThat(menu.map { it.title })
            .doesNotContain(resourceReference(R.string.yield_module_explore_sheet_explore_aave_button_title))
    }

    @Test
    fun `GIVEN yield-supply tx with network fee WHEN convert THEN the fee row is the only one`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.YieldSupply.Topup,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val rows = converter.convert(tx).rows

        // Assert
        assertThat(rows.map { it.label }).containsExactly(resourceReference(R.string.common_network_fee_title))
    }

    // endregion

    // region Fiat line under the amount

    @Test
    fun `GIVEN network fee tx and known rate WHEN convert THEN fiat prices the fee`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.NetworkFee(amount = null, address = USER_ADDRESS),
            isOutgoing = true,
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.000015"), decimals = 18),
        )

        // Act
        val result = onChainConverter(fiatConversion = usdRate("2000")).convert(tx)

        // Assert
        assertThat(result.amountBlock.fiatAmount).isEqualTo(expectedUsd("0.030000"))
    }

    @Test
    fun `GIVEN transfer and known rate WHEN convert THEN fiat prices the amount unsigned`() {
        // Arrange
        val tx = txInfo(type = TransactionType.Transfer, isOutgoing = true, amount = BigDecimal("2"))

        // Act
        val result = onChainConverter(fiatConversion = usdRate("1500")).convert(tx)

        // Assert
        assertThat(result.amountBlock.fiatAmount).isEqualTo(expectedUsd("3000"))
    }

    @Test
    fun `GIVEN no known rate WHEN convert THEN no fiat line`() {
        // Act
        val result = converter.convert(txInfo(type = TransactionType.Transfer, isOutgoing = true))

        // Assert
        assertThat(result.amountBlock.fiatAmount).isNull()
    }

    @Test
    fun `GIVEN approve and known rate WHEN convert THEN no fiat line`() {
        // Arrange — the allowance is denominated in the approved token, which the viewed currency's rate cannot price.
        val tx = txInfo(
            type = TransactionType.Approve(
                amount = SdkAmount(currencySymbol = "USDT", value = BigDecimal("100"), decimals = 6),
                address = USER_ADDRESS,
            ),
            isOutgoing = true,
        )

        // Act
        val result = onChainConverter(fiatConversion = usdRate("2000")).convert(tx)

        // Assert
        assertThat(result.amountBlock.fiatAmount).isNull()
    }

    private fun usdRate(rate: String) = FiatConversion(rate = BigDecimal(rate), appCurrency = AppCurrency.Default)

    private fun expectedUsd(value: String) = stringReference(
        BigDecimal(value).format { fiat(fiatCurrencyCode = "USD", fiatCurrencySymbol = "$") },
    )

    // endregion

    // region YieldSupply Send (rendered as a plain transfer)

    @Test
    fun `GIVEN outgoing yield Send WHEN convert THEN up icon, sent title, minus sign, recipient card, no protocol row`() {
        // Arrange
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val result = converter.convert(tx)

        // Assert
        assertThat(result.header.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_up_20))
        assertThat(result.header.title).isEqualTo(resourceReference(R.string.common_sent))
        assertThat(result.amountBlock.amount.resolveString()).startsWith("- ")
        assertThat(result.counterparty?.label).isEqualTo(resourceReference(R.string.send_recipient))
        assertThat(result.counterparty?.avatar)
            .isEqualTo(TxHistoryDetailsUM.CounterpartyAvatar.Address(USER_ADDRESS))
        // The yield "Validator: Aave" protocol row must not appear — only the network-fee row.
        assertThat(result.rows.map { it.label })
            .containsExactly(resourceReference(R.string.common_network_fee_title))
    }

    @Test
    fun `GIVEN incoming yield Send with fee WHEN convert THEN down icon, received title, from-address card, no fee row`() {
        // Arrange — a received transfer's fee belongs to the sender, so it is omitted.
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val result = converter.convert(tx)

        // Assert
        assertThat(result.header.icon).isEqualTo(TxIcon.Vector(Icons.ic_arrow_down_20))
        assertThat(result.header.title).isEqualTo(resourceReference(R.string.common_received))
        assertThat(result.counterparty?.label).isEqualTo(resourceReference(R.string.common_from_address))
        assertThat(result.rows).isEmpty()
    }

    @Test
    fun `GIVEN yield Send withdraw WHEN convert THEN withdrawn title, lightning icon, asset-leading Aave pair, no counterparty`() {
        // Arrange — a withdraw stays a yield operation in details: lightning glyph, Aave-paired icon, no counterparty.
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = true),
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val result = converter.convert(tx)

        // Assert
        assertThat(result.header.title).isEqualTo(resourceReference(R.string.transaction_history_withdrawn))
        assertThat(result.header.icon).isEqualTo(TxIcon.Vector(Icons.ic_lightning_20))
        assertThat(result.amountBlock.amount.resolveString()).startsWith("+ ")
        assertThat(result.amountBlock.label).isNull()
        val pair = result.amountBlock.icon as TxHistoryDetailsUM.AmountIconUM.OverlappingPair
        assertThat(pair.leading).isInstanceOf(TxHistoryDetailsUM.AmountIconUM.Item.Currency::class.java)
        assertThat(pair.trailing).isEqualTo(TxHistoryDetailsUM.AmountIconUM.Item.Resource(R.drawable.img_aave_22))
        assertThat(result.counterparty).isNull()
        assertThat(result.rows.map { it.label })
            .doesNotContain(resourceReference(R.string.staking_validator))
    }

    @Test
    fun `GIVEN incoming non-withdraw yield Send to own account WHEN convert THEN no fee row`() {
        // Arrange — an own transfer is still a receive on the incoming side: the fee row is dropped like on any other.
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = false,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
            fee = SdkAmount(currencySymbol = "ETH", value = BigDecimal("0.0005"), decimals = 18),
        )

        // Act
        val result = ownConverter.convert(tx)

        // Assert
        assertThat(result.rows).isEmpty()
    }

    @Test
    fun `GIVEN non-withdraw yield Send to own account WHEN convert THEN transferred title`() {
        // Arrange
        val ownConverter = onChainConverter(
            lookup = lookupOf(currency.network.id.rawId to mapOf(USER_ADDRESS to ownAccount)),
        )
        val tx = txInfo(
            type = TransactionType.YieldSupply.Send(address = USER_ADDRESS, isYieldSupplyWithdraw = false),
            isOutgoing = true,
            interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
        )

        // Act
        val result = ownConverter.convert(tx)

        // Assert
        assertThat(result.header.title).isEqualTo(resourceReference(R.string.common_transferred))
    }

    // endregion

    private companion object {
        const val USDC_CONTRACT = "0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val UNKNOWN_VALIDATOR_ADDRESS = "BbM5kJgrwEj3tYFfBPnjcARB54wDUHkXmLUTkazUmt2x"
    }
}