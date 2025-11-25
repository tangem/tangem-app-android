package com.tangem.domain.walletconnect.model

/**
 * Bitcoin WalletConnect method names.
 *
 * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc">Bitcoin RPC Reference</a>
 */
enum class WcBitcoinMethodName(override val raw: String) : WcMethodName {
    SendTransfer("sendTransfer"),
    GetAccountAddresses("getAccountAddresses"),
    SignPsbt("signPsbt"),
    SignMessage("signMessage"),
}

/**
 * Bitcoin WalletConnect methods.
 */
sealed interface WcBitcoinMethod : WcMethod {
    val methodName: String

    /**
     * Send a Bitcoin transfer transaction.
     *
     * @property account Source address (SegWit)
     * @property recipientAddress Destination address
     * @property amount Amount in satoshis
     * @property memo Optional OP_RETURN memo
     * @property changeAddress Optional custom change address
     */
    data class SendTransfer(
        val account: String,
        val recipientAddress: String,
        val amount: String,
        val memo: String?,
        val changeAddress: String?,
    ) : WcBitcoinMethod {
        override val methodName: String = WcBitcoinMethodName.SendTransfer.raw
    }

    /**
     * Get account addresses filtered by intention.
     *
     * @property account Connected account address
     * @property intentions Optional filter ("payment", "ordinal")
     */
    data class GetAccountAddresses(
        val account: String,
        val intentions: List<String>?,
    ) : WcBitcoinMethod {
        override val methodName: String = WcBitcoinMethodName.GetAccountAddresses.raw
    }

    /**
     * Sign a PSBT (BIP-174).
     *
     * @property psbt PSBT in Base64 encoding
     * @property signInputs List of inputs to sign
     * @property broadcast Whether to broadcast after signing
     */
    data class SignPsbt(
        val psbt: String,
        val signInputs: List<SignInput>,
        val broadcast: Boolean,
    ) : WcBitcoinMethod {
        override val methodName: String = WcBitcoinMethodName.SignPsbt.raw
    }

    /**
     * Sign input specification for PSBT.
     */
    data class SignInput(
        val address: String,
        val index: Int,
        val sighashTypes: List<Int>?,
    )

    /**
     * Sign an arbitrary message using Bitcoin message signing format.
     *
     * @property account Connected account address
     * @property message Message to sign
     * @property address Optional specific address to sign with
     * @property protocol Signing protocol ("ecdsa" or "bip322")
     */
    data class SignMessage(
        val account: String,
        val message: String,
        val address: String?,
        val protocol: String,
    ) : WcBitcoinMethod {
        override val methodName: String = WcBitcoinMethodName.SignMessage.raw
    }
}