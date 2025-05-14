package com.tangem.common.test.domain.walletmanager

import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.Address
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.CryptoCurrencyTransaction
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class MockUpdateWalletManagerResultFactory {

    fun createUnreachable(): UpdateWalletManagerResult {
        return UpdateWalletManagerResult.Unreachable(selectedAddress = null, addresses = null)
    }

    fun createUnreachableWithAddress(): UpdateWalletManagerResult {
        return UpdateWalletManagerResult.Unreachable(
            selectedAddress = "0x1",
            addresses = setOf(Address(value = "0x1", type = Address.Type.Primary)),
        )
    }

    fun createNoAccount(): UpdateWalletManagerResult {
        return UpdateWalletManagerResult.NoAccount(
            selectedAddress = "0x1",
            addresses = setOf(Address(value = "0x1", type = Address.Type.Primary)),
            amountToCreateAccount = BigDecimal.ZERO,
            errorMessage = "",
        )
    }

    fun createVerified(): UpdateWalletManagerResult {
        return UpdateWalletManagerResult.Verified(
            selectedAddress = "0x1",
            addresses = setOf(Address(value = "0x1", type = Address.Type.Primary)),
            currenciesAmounts = setOf(
                CryptoCurrencyAmount.Coin(value = BigDecimal.ONE),
            ),
            currentTransactions = setOf(
                CryptoCurrencyTransaction.Coin(txHistoryItem),
            ),
        )
    }

    private companion object {

        val txHistoryItem = TxHistoryItem(
            txHash = "erroribus",
            timestampInMillis = 2771,
            isOutgoing = false,
            destinationType = TxHistoryItem.DestinationType.Single(
                addressType = TxHistoryItem.AddressType.User(address = "0x1"),
            ),
            sourceType = TxHistoryItem.SourceType.Single(address = "0x2"),
            interactionAddressType = null,
            status = TxHistoryItem.TransactionStatus.Confirmed,
            type = TxHistoryItem.TransactionType.Transfer,
            amount = BigDecimal.ONE,
        )
    }
}