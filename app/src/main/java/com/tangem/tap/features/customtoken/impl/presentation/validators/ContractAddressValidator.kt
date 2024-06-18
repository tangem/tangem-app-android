package com.tangem.tap.features.customtoken.impl.presentation.validators

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.tokens.error.AddCustomTokenError

/**
 * Validator of contract address
 *
[REDACTED_AUTHOR]
 */
object ContractAddressValidator {

    /** Validate a [address] using [blockchain] */
    fun validate(address: String, blockchain: Blockchain): ContractAddressValidatorResult {
        return when {
            address.isEmpty() -> ContractAddressValidatorResult.Error(type = AddCustomTokenError.FIELD_IS_EMPTY)
            validateAddress(blockchain, address) -> ContractAddressValidatorResult.Success
            else -> ContractAddressValidatorResult.Error(type = AddCustomTokenError.INVALID_CONTRACT_ADDRESS)
        }
    }

    private fun validateAddress(blockchain: Blockchain, address: String): Boolean {
        return when (blockchain) {
            Blockchain.Unknown,
            Blockchain.Binance,
            Blockchain.BinanceTestnet,
            Blockchain.Cardano,
            -> SuccessAddressValidator.validate(address)
            else -> blockchain.validateAddress(address)
        }
    }

    private object SuccessAddressValidator : AddressService() {
        override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
            throw UnsupportedOperationException()
        }

        override fun validate(address: String): Boolean = true
    }
}