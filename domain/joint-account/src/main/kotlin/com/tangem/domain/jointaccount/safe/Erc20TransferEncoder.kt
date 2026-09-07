package com.tangem.domain.jointaccount.safe

import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import java.math.BigInteger

/**
 * Encodes the callData of an ERC-20 `transfer(address,uint256)`. The only place in the joint-account
 * code that knows what an ERC-20 transfer looks like — everything downstream treats the callData as opaque.
 */
object Erc20TransferEncoder {

    private const val TRANSFER_SELECTOR = "a9059cbb"

    /** `0x`-prefixed callData of `transfer(recipient, amount)`, lowercase */
    fun encodeTransfer(recipient: String, amount: BigInteger): String {
        val callData = TRANSFER_SELECTOR.hexToBytes() +
            recipient.toAddressWord() +
            amount.toUint256Word()

        return "0x" + callData.toHexString().lowercase()
    }
}