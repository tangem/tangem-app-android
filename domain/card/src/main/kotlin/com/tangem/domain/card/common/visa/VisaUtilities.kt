package com.tangem.domain.card.common.visa

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.moneyaccount.derivation.MONEY_ACCOUNT_DERIVATION_PATH
import java.util.Locale

object VisaUtilities {

    val visaBlockchain = Blockchain.Polygon

    val customDerivationPath = DerivationPath("m/44'/60'/999999'/0/0")
    val moneyAccountDerivationPath = MONEY_ACCOUNT_DERIVATION_PATH
    val curve = EllipticCurve.Secp256k1

    fun signWithNonceMessage(nonce: String): String {
        return "Tangem Pay wants to sign in with your account. Nonce: $nonce"
    }

    fun moneyAccountDepositAddressMessage(depositAddress: String): String {
        return "Banking account deposit address ${depositAddress.lowercase(Locale.US)}"
    }

    // TODO: [REDACTED_TASK_KEY] - Get this public function from Blockchain SDK
    fun hashPersonalMessage(message: ByteArray): ByteArray {
        val prefix = "\u0019Ethereum Signed Message:\n${message.size}".toByteArray()
        return (prefix + message).toKeccak()
    }

    fun generateAddressFromExtendedKey(extendedPublicKey: ExtendedPublicKey): String {
        val derivationData = visaBlockchain.makeAddressesFromExtendedPublicKey(
            extendedPublicKey = extendedPublicKey,
            rawPath = null, // if null, the function will use default derivation path for the blockchain
            cachedIndex = null,
        )
        return derivationData.address
    }

    fun unmarshallSignature(signature: ByteArray, hash: ByteArray, extendedPublicKey: ExtendedPublicKey): String {
        return UnmarshalHelper.unmarshalSignatureExtended(
            signature = signature,
            hash = hash,
            publicKey = extendedPublicKey.publicKey.toDecompressedPublicKey(),
        ).asRSVLegacyEVM().toHexString().lowercase()
    }
}