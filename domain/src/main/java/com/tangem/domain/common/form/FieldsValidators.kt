package com.tangem.domain.common.form

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.solana.SolanaAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.Validator
import com.tangem.domain.AddCustomTokenError

/**
[REDACTED_AUTHOR]
 */
abstract class CustomTokenValidator<T> : Validator<T, AddCustomTokenError>

class StringIsEmptyValidator : CustomTokenValidator<String>() {
    override fun validate(data: String?): AddCustomTokenError? = when {
        data == null || data.isEmpty() -> null
        else -> AddCustomTokenError.FieldIsNotEmpty
    }
}

class StringIsNotEmptyValidator : CustomTokenValidator<String>() {
    override fun validate(data: String?): AddCustomTokenError? = when {
        data == null || data.isEmpty() -> AddCustomTokenError.FieldIsEmpty
        else -> null
    }
}

class TokenContractAddressValidator : CustomTokenValidator<String>() {

    private var blockchain: Blockchain = Blockchain.Unknown

    fun nextValidationFor(blockchain: Blockchain) {
        this.blockchain = blockchain
    }

    override fun validate(data: String?): AddCustomTokenError? {
        if (data == null || data.isEmpty()) return AddCustomTokenError.FieldIsEmpty

        return if (getAddressService().validate(data)) {
            null
        } else {
            AddCustomTokenError.InvalidContractAddress
        }
    }

    private fun getAddressService(): AddressService {
        return when (blockchain) {
            Blockchain.Solana, Blockchain.SolanaTestnet -> SolanaAddressService()
            Blockchain.Unknown -> EthereumAddressService()
            else -> {
                if (blockchain.isEvm()) {
                    EthereumAddressService()
                } else {
                    throw UnsupportedOperationException()
                }
            }
        }
    }
}

class TokenNetworkValidator : CustomTokenValidator<Blockchain>() {
    override fun validate(data: Blockchain?): AddCustomTokenError? = when (data) {
        null, Blockchain.Unknown -> AddCustomTokenError.NetworkIsNotSelected
        else -> null
    }
}

class TokenNameValidator : CustomTokenValidator<String>() {
    override fun validate(data: String?): AddCustomTokenError? = StringIsNotEmptyValidator().validate(data)
}

class TokenSymbolValidator : CustomTokenValidator<String>() {
    override fun validate(data: String?): AddCustomTokenError? = StringIsNotEmptyValidator().validate(data)
}

class TokenDecimalsValidator : CustomTokenValidator<String>() {
    override fun validate(data: String?): AddCustomTokenError? {
        val decimal = data?.toIntOrNull() ?: return AddCustomTokenError.FieldIsEmpty

        return when {
            decimal > 30 -> AddCustomTokenError.InvalidDecimalsCount
            else -> null
        }
    }
}