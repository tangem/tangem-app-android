package com.tangem.domain.common.visa

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.scan.CardDTO

object VisaWalletPublicKeyUtility {

    fun validateExtendedPublicKey(targetAddress: String, extendedPublicKey: ExtendedPublicKey): Either<Error, Unit> =
        either {
            validatePublicKey(
                targetAddress = targetAddress,
                publicKey = extendedPublicKey.publicKey,
            ).bind()
        }

    fun findKeyWithoutDerivation(targetAddress: String, card: CardDTO): Either<Error, ByteArray> = either {
        val wallet = findWalletOnVisaCurve(card).bind()

        validatePublicKey(
            targetAddress = targetAddress,
            publicKey = wallet.publicKey,
        ).bind()

        wallet.publicKey
    }

    fun generateAddressOnVisaCurve(walletPublicKey: ByteArray): Either<Error, Address> = either {
        val addresses = catch(
            block = {
                VisaUtilities.visaBlockchain.makeAddresses(
                    walletPublicKey = walletPublicKey,
                    pairPublicKey = null,
                    curve = VisaUtilities.mandatoryCurve,
                )
            },
            catch = { raise(Error.FailedToCreateAddress) },
        )

        addresses.firstOrNull { it.type == AddressType.Default } ?: raise(Error.FailedToCreateAddress)
    }

    private fun findWalletOnVisaCurve(card: CardDTO): Either<Error, CardDTO.Wallet> = either {
        card.wallets.firstOrNull { it.curve == VisaUtilities.mandatoryCurve } ?: raise(Error.MissingWalletOnTargetCurve)
    }

    private fun validatePublicKey(targetAddress: String, publicKey: ByteArray): Either<Error, Unit> = either {
        val address = generateAddressOnVisaCurve(publicKey).bind()

        if (address.value != targetAddress) {
            raise(Error.AddressNotMatched)
        }
    }

    enum class Error(val message: String) {
        AddressNotMatched("ValidationError: Address not matched"),
        FailedToCreateAddress("ValidationError: Failed to create address"),
        MissingWalletOnTargetCurve("ValidationError: Missing wallet on target curve"),
    }
}