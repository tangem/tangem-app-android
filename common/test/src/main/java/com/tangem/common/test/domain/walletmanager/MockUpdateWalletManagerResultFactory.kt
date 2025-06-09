package com.tangem.common.test.domain.walletmanager

import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult.*
import com.tangem.domain.models.network.TxInfo
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class MockUpdateWalletManagerResultFactory {

    fun createUnreachable(): UpdateWalletManagerResult {
        return Unreachable(selectedAddress = null, addresses = null)
    }

    fun createUnreachableWithAddress(): UpdateWalletManagerResult {
        return Unreachable(
            selectedAddress = "0x1",
            addresses = setOf(Address(value = "0x1", type = Address.Type.Primary)),
        )
    }

    fun createNoAccount(): UpdateWalletManagerResult {
        return NoAccount(
            selectedAddress = "0x1",
            addresses = setOf(Address(value = "0x1", type = Address.Type.Primary)),
            amountToCreateAccount = BigDecimal.ONE,
            errorMessage = "",
        )
    }

    fun createVerified(): Verified {
        return Verified(
            selectedAddress = "0x1",
            addresses = setOf(Address(value = "0x1", type = Address.Type.Primary)),
            currenciesAmounts = setOf(
                CryptoCurrencyAmount.Coin(value = BigDecimal.ONE),
            ),
            currentTransactions = setOf(CryptoCurrencyTransaction.Coin(txInfo)),
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