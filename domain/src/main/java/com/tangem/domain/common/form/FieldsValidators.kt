package com.tangem.domain.common.form

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.HDWalletError
import com.tangem.domain.common.Validator
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

    override fun validate(data: String?): AddCustomTokenError? = when {
//        data == null || data.isEmpty() -> AddCustomTokenError.FieldIsEmpty
        else -> EthAddressValidator().validate(data)
    }

    private class EthAddressValidator : CustomTokenValidator<String>() {
        override fun validate(data: String?): AddCustomTokenError? {
            val isValid = EthereumAddressService().validate(data ?: "")
            return if (isValid) null else AddCustomTokenError.InvalidContractAddress
        }
    }
}

class TokenNetworkValidator : CustomTokenValidator<Blockchain>() {
    override fun validate(data: Blockchain?): AddCustomTokenError? = when (data) {
        null, Blockchain.Unknown -> AddCustomTokenError.NetworkIsNotSelected
        else -> null
    }
}

class DerivationPathValidator : CustomTokenValidator<String>() {
    override fun validate(data: String?): AddCustomTokenError? = when {
        data == null || data.isEmpty() -> null
        else -> {
            try {
                DerivationPath(data)
                null
            } catch (ex: HDWalletError) {
                AddCustomTokenError.InvalidDerivationPath
            }
        }
    }
}