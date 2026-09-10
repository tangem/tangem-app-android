package com.tangem.features.send.send.joint

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.safe.Erc20TransferEncoder
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import java.math.BigDecimal
import java.math.BigInteger

internal object JointTransferIntentFactory {

    fun create(amountUM: AmountState, destinationUM: DestinationUM, currency: CryptoCurrency): JointTransferIntent? {
        val amount = (amountUM as? AmountState.Data)?.amountTextField?.cryptoAmount?.value ?: return null
        if (amount <= BigDecimal.ZERO) return null
        val recipient = (destinationUM as? DestinationUM.Content)
            ?.addressTextField?.actualAddress?.takeIf { it.isNotBlank() }
            ?: return null

        val amountWei = amount.movePointRight(currency.decimals).toBigInteger()

        return when (currency) {
            is CryptoCurrency.Coin -> JointTransferIntent.Native(recipient = recipient, amountWei = amountWei)
            is CryptoCurrency.Token -> JointTransferIntent.Erc20(
                tokenContract = currency.contractAddress,
                recipient = recipient,
                amountWei = amountWei,
            )
        }
    }

    fun JointTransferIntent.toSafeInnerCall(): SafeInnerCall = when (this) {
        is JointTransferIntent.Native -> SafeInnerCall(
            to = recipient.lowercase(),
            value = amountWei,
            data = "0x",
        )
        is JointTransferIntent.Erc20 -> SafeInnerCall(
            to = tokenContract.lowercase(),
            value = BigInteger.ZERO,
            data = Erc20TransferEncoder.encodeTransfer(recipient = recipient, amount = amountWei),
        )
    }
}