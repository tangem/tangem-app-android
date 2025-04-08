package com.tangem.domain.common.visa

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.visa.error.VisaActivationError

object VisaWalletPublicKeyUtility {

    fun validateExtendedPublicKey(
        targetAddress: String,
        extendedPublicKey: ExtendedPublicKey,
    ): Either<VisaActivationError, Unit> = either {
        validatePublicKey(
            targetAddress = targetAddress,
            publicKey = extendedPublicKey.publicKey,
        ).bind()
    }

    fun findKeyWithoutDerivation(targetAddress: String, card: CardDTO): Either<VisaActivationError, ByteArray> =
        either {
            val wallet = findWalletOnSecp256k1(card).bind()

            validatePublicKey(
                targetAddress = targetAddress,
                publicKey = wallet.publicKey,
            ).bind()

            wallet.publicKey
        }

    fun generateAddressOnSecp256k1(walletPublicKey: ByteArray): Either<VisaActivationError, Address> = either {
        val addresses = catch(
            block = {
                VisaUtilities.visaBlockchain.makeAddresses(
                    walletPublicKey = walletPublicKey,
                    pairPublicKey = null,
                    curve = EllipticCurve.Secp256k1,
                )
            },
            catch = { raise(VisaActivationError.FailedToCreateAddress) },
        )

        addresses.firstOrNull { it.type == AddressType.Default } ?: raise(VisaActivationError.FailedToCreateAddress)
    }

    private fun findWalletOnSecp256k1(card: CardDTO): Either<VisaActivationError, CardDTO.Wallet> = either {
        card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: raise(VisaActivationError.MissingWallet)
    }

    private fun validatePublicKey(targetAddress: String, publicKey: ByteArray): Either<VisaActivationError, Unit> =
        either {
            val address = generateAddressOnSecp256k1(publicKey).bind()

            if (address.value != targetAddress) {
                raise(VisaActivationError.AddressNotMatched)
            }
        }
}