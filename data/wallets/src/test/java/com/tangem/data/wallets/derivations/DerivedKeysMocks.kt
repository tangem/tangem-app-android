package com.tangem.data.wallets.derivations

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationConfigV2
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
[REDACTED_AUTHOR]
 */
internal object DerivedKeysMocks {

    val ethereumDerivedKeys = mapOf(
        EllipticCurve.Secp256k1.name.toByteArray().toMapKey() to ExtendedPublicKeysMap(
            mapOf(
                DerivationConfigV2.derivations(Blockchain.Ethereum).values.first() to ExtendedPublicKey(
                    publicKey = ByteArray(0),
                    chainCode = ByteArray(0),
                    depth = 2646,
                    parentFingerprint = ByteArray(0),
                    childNumber = 1142,
                ),
            ),
        ),
    )
}