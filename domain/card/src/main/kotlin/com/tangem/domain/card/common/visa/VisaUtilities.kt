package com.tangem.domain.card.common.visa

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.scan.CardDTO

private const val VISA_BATCH_START = "AE"
private const val VISA_BATCH_START_2 = "FFFC"

object VisaUtilities {

    val visaBlockchain = Blockchain.Polygon

    val visaDefaultDerivationPath
        get() = visaBlockchain.derivationPath(DerivationStyle.V3)
    val customDerivationPath = DerivationPath("m/44'/60'/999999'/0/0")
    val curve = EllipticCurve.Secp256k1

    fun signWithNonceMessage(nonce: String): String {
        return "Tangem Pay wants to sign in with your account. Nonce: $nonce"
    }

    fun isVisaCard(card: CardDTO): Boolean {
        return isVisaCard(card.firmwareVersion.doubleValue, card.batchId)
    }

    fun isVisaCard(firmwareVersion: Double, batchId: String): Boolean {
        return firmwareVersion in FirmwareVersion.visaRange &&
            (batchId.startsWith(VISA_BATCH_START) || batchId.startsWith(VISA_BATCH_START_2))
    }

    // TODO: [REDACTED_TASK_KEY] - Get this public function from Blockchain SDK
    fun hashPersonalMessage(message: ByteArray): ByteArray {
        val prefix = "\u0019Ethereum Signed Message:\n${message.size}".toByteArray()
        return (prefix + message).toKeccak()
    }

    fun generateAddressFromExtendedKey(extendedPublicKey: ExtendedPublicKey): String {
        val derivationData = visaBlockchain.makeAddressesFromExtendedPublicKey(
            extendedPublicKey = extendedPublicKey,
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