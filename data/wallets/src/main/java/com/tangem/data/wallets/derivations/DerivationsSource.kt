package com.tangem.data.wallets.derivations

import com.tangem.common.card.EllipticCurve
import com.tangem.domain.card.common.TapWorkarounds.hasOldStyleDerivation
import com.tangem.domain.models.scan.KeyWalletPublicKey
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.config.ColdCurvesConfig
import com.tangem.domain.wallets.config.CurvesConfig
import com.tangem.domain.wallets.config.curvesConfig
import com.tangem.domain.wallets.derivations.DerivationStyleProvider
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * Source of derivations data
 */
internal sealed interface DerivationsSource {

    val isHDWalletAllowed: Boolean
    val hasOldStyleDerivation: Boolean
    val curvesConfig: CurvesConfig
    val derivationStyleProvider: DerivationStyleProvider

    fun getWalletPublicKey(curve: EllipticCurve): ByteArray?
    fun getDerivedKeys(publicKey: KeyWalletPublicKey): ExtendedPublicKeysMap

    data class FromUserWallet(val userWallet: UserWallet) : DerivationsSource {
        override val isHDWalletAllowed: Boolean
            get() = when (userWallet) {
                is UserWallet.Cold -> userWallet.scanResponse.card.settings.isHDWalletAllowed
                is UserWallet.Hot -> true
            }

        override val hasOldStyleDerivation: Boolean
            get() = when (userWallet) {
                is UserWallet.Cold -> userWallet.scanResponse.card.hasOldStyleDerivation
                is UserWallet.Hot -> false
            }

        override val curvesConfig: CurvesConfig
            get() = userWallet.curvesConfig

        override val derivationStyleProvider: DerivationStyleProvider
            get() = userWallet.derivationStyleProvider

        override fun getWalletPublicKey(curve: EllipticCurve): ByteArray? {
            return when (userWallet) {
                is UserWallet.Cold -> userWallet.scanResponse.getWalletPublicKey(curve)
                is UserWallet.Hot -> userWallet.wallets
                    ?.firstOrNull { it.curve == curve && it.chainCode != null }
                    ?.publicKey
            }
        }

        override fun getDerivedKeys(publicKey: KeyWalletPublicKey): ExtendedPublicKeysMap {
            return when (userWallet) {
                is UserWallet.Cold -> userWallet.scanResponse.getDerivedKeys(publicKey)
                is UserWallet.Hot -> {
                    val derivedKeys = userWallet.wallets
                        ?.firstOrNull { it.publicKey.contentEquals(publicKey.bytes) }
                        ?.derivedKeys
                        .orEmpty()

                    ExtendedPublicKeysMap(derivedKeys)
                }
            }
        }
    }

    data class FromScanResponse(val scanResponse: ScanResponse) : DerivationsSource {
        override val isHDWalletAllowed: Boolean
            get() = scanResponse.card.settings.isHDWalletAllowed

        override val hasOldStyleDerivation: Boolean
            get() = scanResponse.card.hasOldStyleDerivation

        override val curvesConfig: CurvesConfig
            get() = ColdCurvesConfig(scanResponse.card)

        override val derivationStyleProvider: DerivationStyleProvider
            get() = scanResponse.derivationStyleProvider

        override fun getWalletPublicKey(curve: EllipticCurve): ByteArray? {
            return scanResponse.getWalletPublicKey(curve)
        }

        override fun getDerivedKeys(publicKey: KeyWalletPublicKey): ExtendedPublicKeysMap {
            return scanResponse.getDerivedKeys(publicKey)
        }
    }
}

private fun ScanResponse.getWalletPublicKey(curve: EllipticCurve): ByteArray? {
    return card.wallets.firstOrNull { it.curve == curve && it.chainCode != null }
        ?.publicKey
}

private fun ScanResponse.getDerivedKeys(publicKey: KeyWalletPublicKey): ExtendedPublicKeysMap {
    return derivedKeys[publicKey] ?: ExtendedPublicKeysMap(emptyMap())
}