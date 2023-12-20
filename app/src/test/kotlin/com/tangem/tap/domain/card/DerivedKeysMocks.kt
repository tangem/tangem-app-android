package com.tangem.tap.domain.card

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationConfigV2
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * @author Andrew Khokhlov on 20/12/2023
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
