package com.tangem.tap.features.customtoken.impl.presentation.validators

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.AddCustomTokenError

/**
 * Validator of contract address
 *
 * @author Andrew Khokhlov on 22/04/2023
 */
object ContactAddressValidator {

    /** Validate a [address] using [blockchain] */
    fun validate(address: String, blockchain: Blockchain): ContractAddressValidatorResult {
        return when {
            address.isEmpty() -> ContractAddressValidatorResult.Error(type = AddCustomTokenError.FieldIsEmpty)
            validateAddress(blockchain, address) -> ContractAddressValidatorResult.Success
            else -> ContractAddressValidatorResult.Error(type = AddCustomTokenError.InvalidContractAddress)
        }
    }

    private fun validateAddress(blockchain: Blockchain, address: String): Boolean {
        return when (blockchain) {
            Blockchain.Unknown, Blockchain.Binance, Blockchain.BinanceTestnet -> {
                SuccessAddressValidator.validate(address)
            }
            else -> {
                blockchain.validateAddress(address)
            }
        }
    }

    private object SuccessAddressValidator : AddressService() {
        override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
            throw UnsupportedOperationException()
        }

        override fun validate(address: String): Boolean = true
    }
}
