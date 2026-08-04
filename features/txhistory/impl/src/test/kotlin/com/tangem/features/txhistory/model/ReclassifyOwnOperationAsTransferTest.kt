package com.tangem.features.txhistory.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.Account.CryptoPortfolio.Companion.createMainAccount
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Unit tests for [reclassifyOwnOperationAsTransfer]: an unrecognized contract call (Operation / UnknownOperation) is
 * rewritten to [TransactionType.Transfer] only when its direction-correct counterparty resolves to one of the user's own
 * portfolios. The direction cases pin the "trap": the own side must be the counterparty (recipient for outgoing, sender
 * for incoming), not the viewed wallet's own side.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReclassifyOwnOperationAsTransferTest {

    @ParameterizedTest
    @ProvideTestModels
    fun reclassify(model: Model) {
        // Act
        val result = model.tx.reclassifyOwnOperationAsTransfer(model.lookup, NETWORK_ID)

        // Assert
        assertThat(result).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<Model> {
        val outgoingOperationOwnDest = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            destinationAddress = OWN_ADDRESS,
            sourceAddress = EXTERNAL_ADDRESS,
        )
        val incomingUnknownOwnSource = txInfo(
            type = TransactionType.UnknownOperation,
            isOutgoing = false,
            destinationAddress = EXTERNAL_ADDRESS,
            sourceAddress = OWN_ADDRESS,
        )
        val incomingOperationOwnDestOnly = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = false,
            destinationAddress = OWN_ADDRESS,
            sourceAddress = EXTERNAL_ADDRESS,
        )
        val outgoingOperationExternalDest = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            destinationAddress = EXTERNAL_ADDRESS,
            sourceAddress = OWN_ADDRESS,
        )
        val alreadyTransfer = txInfo(
            type = TransactionType.Transfer,
            isOutgoing = true,
            destinationAddress = OWN_ADDRESS,
        )
        val outgoingOperationMultipleDest = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = true,
            destinationType = TxInfo.DestinationType.Multiple(
                addressTypes = listOf(TxInfo.AddressType.User(OWN_ADDRESS), TxInfo.AddressType.User(EXTERNAL_ADDRESS)),
            ),
        )
        val incomingOperationMultipleSource = txInfo(
            type = TransactionType.Operation(name = "Mint"),
            isOutgoing = false,
            sourceType = TxInfo.SourceType.Multiple(addresses = listOf(OWN_ADDRESS, EXTERNAL_ADDRESS)),
        )

        return listOf(
            // Own counterparty on the correct side -> reclassified, interaction normalized to that counterparty.
            Model(
                name = "outgoing Operation, own recipient -> Transfer",
                tx = outgoingOperationOwnDest,
                lookup = ownLookup(OWN_ADDRESS),
                expected = outgoingOperationOwnDest.copy(
                    type = TransactionType.Transfer,
                    interactionAddressType = TxInfo.InteractionAddressType.User(OWN_ADDRESS),
                ),
            ),
            Model(
                name = "incoming UnknownOperation, own sender -> Transfer",
                tx = incomingUnknownOwnSource,
                lookup = ownLookup(OWN_ADDRESS),
                expected = incomingUnknownOwnSource.copy(
                    type = TransactionType.Transfer,
                    interactionAddressType = TxInfo.InteractionAddressType.User(OWN_ADDRESS),
                ),
            ),
            // The trap: incoming tx whose own address is the destination (the viewed wallet), sender external -> unchanged.
            Model(
                name = "incoming Operation, own only on destination side -> unchanged",
                tx = incomingOperationOwnDestOnly,
                lookup = ownLookup(OWN_ADDRESS),
                expected = incomingOperationOwnDestOnly,
            ),
            // External counterparty -> unchanged.
            Model(
                name = "outgoing Operation, external recipient -> unchanged",
                tx = outgoingOperationExternalDest,
                lookup = ownLookup(OWN_ADDRESS),
                expected = outgoingOperationExternalDest,
            ),
            // Not an unrecognized call -> guard returns it untouched even with an own counterparty.
            Model(
                name = "already Transfer -> unchanged",
                tx = alreadyTransfer,
                lookup = ownLookup(OWN_ADDRESS),
                expected = alreadyTransfer,
            ),
            // Multi-address side -> no single counterparty to pin -> unchanged.
            Model(
                name = "outgoing Operation, multiple recipients -> unchanged",
                tx = outgoingOperationMultipleDest,
                lookup = ownLookup(OWN_ADDRESS),
                expected = outgoingOperationMultipleDest,
            ),
            Model(
                name = "incoming Operation, multiple senders -> unchanged",
                tx = incomingOperationMultipleSource,
                lookup = ownLookup(OWN_ADDRESS),
                expected = incomingOperationMultipleSource,
            ),
        )
    }

    private fun ownLookup(vararg ownAddresses: String): TxHistoryLookupContext {
        val account = createMainAccount(UserWalletId(stringValue = "00"))
        return TxHistoryLookupContext(
            ownAccountByNetwork = mapOf(NETWORK_ID to ownAddresses.associateWith { account }),
            isAccountsModeEnabled = true,
            walletInfoById = emptyMap(),
        )
    }

    private fun txInfo(
        type: TransactionType,
        isOutgoing: Boolean = false,
        sourceAddress: String = OWN_ADDRESS,
        destinationAddress: String = OWN_ADDRESS,
        sourceType: TxInfo.SourceType = TxInfo.SourceType.Single(address = sourceAddress),
        destinationType: TxInfo.DestinationType =
            TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(destinationAddress)),
    ): TxInfo = TxInfo(
        txHash = "0xtxhash",
        timestampInMillis = 1_700_000_000_000L,
        isOutgoing = isOutgoing,
        destinationType = destinationType,
        sourceType = sourceType,
        interactionAddressType = null,
        status = TxInfo.TransactionStatus.Confirmed,
        type = type,
        amount = BigDecimal.ONE,
    )

    internal data class Model(
        val name: String,
        val tx: TxInfo,
        val lookup: TxHistoryLookupContext,
        val expected: TxInfo,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val NETWORK_ID = Network.RawID(value = "ethereum")
        const val OWN_ADDRESS = "0xOwnAddress1234"
        const val EXTERNAL_ADDRESS = "0xExternalAddress5678"
    }
}