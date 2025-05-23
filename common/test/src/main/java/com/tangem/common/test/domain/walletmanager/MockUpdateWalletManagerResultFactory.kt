package com.tangem.common.test.domain.walletmanager

import com.tangem.domain.models.network.TxInfo
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
            amountToCreateAccount = BigDecimal.ONE,
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
                CryptoCurrencyTransaction.Coin(txInfo),
            ),
        )
    }

    private companion object {

        val txInfo = TxInfo(
            txHash = "erroribus",
            timestampInMillis = 2771,
            isOutgoing = false,
            destinationType = TxInfo.DestinationType.Single(
                addressType = TxInfo.AddressType.User(address = "0x1"),
            ),
            sourceType = TxInfo.SourceType.Single(address = "0x2"),
            interactionAddressType = null,
            status = TxInfo.TransactionStatus.Confirmed,
            type = TxInfo.TransactionType.Transfer,
            amount = BigDecimal.ONE,
        )
    }
}