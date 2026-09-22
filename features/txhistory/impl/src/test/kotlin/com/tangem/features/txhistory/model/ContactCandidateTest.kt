package com.tangem.features.txhistory.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.converter.TxDetailsConverterTestBase
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Unit tests for [contactCandidate] — which counterparty the details screen offers to save to the address book. Pins
 * the type filter (plain transfers and an outgoing swap's payout only), that an own portfolio is never offered, and
 * that the "already saved" check is scoped to the wallet the transaction belongs to.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContactCandidateTest : TxDetailsConverterTestBase() {

    private val ethereumId: Network.RawID get() = currency.network.id.rawId
    private val bitcoinId: Network.RawID get() = bitcoin.network.id.rawId

    @ParameterizedTest
    @ProvideTestModels
    fun contactCandidate(model: Model) {
        // Act
        val actual = model.tx.contactCandidate(currency = currency, lookup = model.lookup)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Suppress("LongMethod")
    private fun provideTestModels(): List<Model> = listOf(
        Model(
            name = "outgoing transfer to an unsaved external address -> offered",
            tx = transfer(isOutgoing = true),
            lookup = lookupOf(),
            expected = ContactCandidate(address = USER_ADDRESS, networkId = ethereumId),
        ),
        Model(
            name = "incoming transfer from an unsaved external address -> offered",
            tx = transfer(isOutgoing = false),
            lookup = lookupOf(),
            expected = ContactCandidate(address = USER_ADDRESS, networkId = ethereumId),
        ),
        Model(
            name = "address already saved in the transaction's wallet -> not offered",
            tx = transfer(isOutgoing = true),
            lookup = lookupOf(contacts = listOf(contactOf(MockAccounts.userWalletId))),
            expected = null,
        ),
        Model(
            name = "address saved only in another wallet -> offered",
            tx = transfer(isOutgoing = true),
            lookup = lookupOf(contacts = listOf(contactOf(OTHER_WALLET_ID))),
            expected = ContactCandidate(address = USER_ADDRESS, networkId = ethereumId),
        ),
        Model(
            name = "own account -> not offered",
            tx = transfer(isOutgoing = true),
            lookup = lookupOf(ethereumId to mapOf(USER_ADDRESS to ownAccount)),
            expected = null,
        ),
        Model(
            name = "no wallet in the lookup context -> not offered",
            tx = transfer(isOutgoing = true),
            lookup = lookupOf().copy(viewedWalletId = null),
            expected = null,
        ),
        Model(
            name = "non-withdraw yield Send is a plain transfer -> offered",
            tx = onChain(
                type = TransactionType.YieldSupply.Send(isYieldSupplyWithdraw = false),
                isOutgoing = true,
                interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
            ),
            lookup = lookupOf(),
            expected = ContactCandidate(address = USER_ADDRESS, networkId = ethereumId),
        ),
        Model(
            name = "yield withdraw -> not offered",
            tx = onChain(
                type = TransactionType.YieldSupply.Send(isYieldSupplyWithdraw = true),
                isOutgoing = true,
                interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
            ),
            lookup = lookupOf(),
            expected = null,
        ),
        Model(
            name = "unrecognized contract call -> not offered",
            tx = onChain(
                type = TransactionType.Operation(name = "Mint NFT"),
                isOutgoing = true,
                interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
            ),
            lookup = lookupOf(),
            expected = null,
        ),
        Model(
            name = "approve -> not offered",
            tx = onChain(
                type = TransactionType.Approve(
                    amount = SdkAmount(currencySymbol = "USDT", value = null, decimals = 6),
                    address = USER_ADDRESS,
                ),
                isOutgoing = true,
                interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
            ),
            lookup = lookupOf(),
            expected = null,
        ),
        Model(
            name = "staking -> not offered",
            tx = onChain(
                type = TransactionType.Staking.Stake,
                isOutgoing = true,
                interactionAddressType = TxInfo.InteractionAddressType.Validator(VALIDATOR_ADDRESS),
            ),
            lookup = lookupOf(),
            expected = null,
        ),
        Model(
            name = "transfer with a contract interaction address -> not offered",
            tx = onChain(
                type = TransactionType.Transfer,
                isOutgoing = true,
                interactionAddressType = TxInfo.InteractionAddressType.Contract(USER_ADDRESS),
            ),
            lookup = lookupOf(),
            expected = null,
        ),
        Model(
            name = "transfer to multiple addresses -> not offered",
            tx = onChain(
                type = TransactionType.Transfer,
                isOutgoing = true,
                interactionAddressType = TxInfo.InteractionAddressType.Multiple(listOf(USER_ADDRESS)),
            ),
            lookup = lookupOf(),
            expected = null,
        ),
        Model(
            name = "outgoing swap paying out to an external address -> offered on the payout network",
            tx = expressSwap(status = ExpressExchangeStatus.Finished, isOutgoing = true),
            lookup = lookupOf(),
            expected = ContactCandidate(address = PAYOUT_ADDRESS, networkId = bitcoinId),
        ),
        Model(
            name = "outgoing swap paying out to an own address -> not offered",
            tx = expressSwap(status = ExpressExchangeStatus.Finished, isOutgoing = true),
            lookup = lookupOf(bitcoinId to mapOf(PAYOUT_ADDRESS to ownAccount)),
            expected = null,
        ),
        Model(
            name = "incoming swap -> not offered",
            tx = expressSwap(status = ExpressExchangeStatus.Finished, isOutgoing = false),
            lookup = lookupOf(),
            expected = null,
        ),
        Model(
            name = "onramp -> not offered",
            tx = expressOnramp(status = ExpressOnrampStatus.Finished),
            lookup = lookupOf(),
            expected = null,
        ),
    )

    private fun transfer(isOutgoing: Boolean) = onChain(
        type = TransactionType.Transfer,
        isOutgoing = isOutgoing,
        interactionAddressType = TxInfo.InteractionAddressType.User(USER_ADDRESS),
    )

    private fun contactOf(walletId: UserWalletId) = TestContacts.create(
        name = "Binance",
        address = USER_ADDRESS,
        walletId = walletId,
        networkId = ethereumId,
    )

    internal data class Model(
        val name: String,
        val tx: TxHistoryInfo,
        val lookup: TxHistoryLookupContext,
        val expected: ContactCandidate?,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val OTHER_WALLET_ID = UserWalletId("022")
    }
}