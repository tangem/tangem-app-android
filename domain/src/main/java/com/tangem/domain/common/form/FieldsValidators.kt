package com.tangem.domain.common.form

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.Validator
import com.tangem.domain.features.addCustomToken.AddCustomTokenError

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
    override fun validate(data: String?): AddCustomTokenError? {
        if (data == null || data.isEmpty()) return null

        return if (EthereumAddressService().validate(data)) {
            null
        } else {
            AddCustomTokenError.InvalidContractAddress
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