package com.tangem.domain.common.form

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.solana.SolanaAddressService
import com.tangem.blockchain.blockchains.tron.TronAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressService
import com.tangem.common.Validator
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.AddCustomTokenError
import timber.log.Timber

/**
 * Created by Anton Zhilenkov on 25/03/2022.
 */
interface CustomTokenValidator<T> : Validator<T, AddCustomTokenError>

class StringIsEmptyValidator : CustomTokenValidator<String> {
    override fun validate(data: String?): AddCustomTokenError? {
        return if (data.isNullOrEmpty()) null else AddCustomTokenError.FieldIsNotEmpty
    }
}

class StringIsNotEmptyValidator : CustomTokenValidator<String> {
    override fun validate(data: String?): AddCustomTokenError? {
        return if (data.isNullOrEmpty()) AddCustomTokenError.FieldIsEmpty else null
    }
}

class TokenContractAddressValidator : CustomTokenValidator<String> {

    private var blockchain: Blockchain = Blockchain.Unknown

    private val successAddressValidator = object : AddressService() {
        override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
            throw UnsupportedOperationException()
        }

        override fun validate(address: String): Boolean = true
    }

    fun nextValidationFor(blockchain: Blockchain) {
        this.blockchain = blockchain
    }

    override fun validate(data: String?): AddCustomTokenError? {
        return when {
            data.isNullOrEmpty() -> AddCustomTokenError.FieldIsEmpty
            getAddressService().validate(data) -> null
            else -> AddCustomTokenError.InvalidContractAddress
        }
    }

    private fun getAddressService(): AddressService {
        return when (blockchain) {
            Blockchain.Unknown -> successAddressValidator
            Blockchain.Binance, Blockchain.BinanceTestnet -> successAddressValidator
            Blockchain.Solana, Blockchain.SolanaTestnet -> SolanaAddressService()
            Blockchain.Tron, Blockchain.TronTestnet -> TronAddressService()
            else -> {
                if (blockchain.isEvm()) {
                    EthereumAddressService()
                } else {
                    Timber.e("Throw for blockchain: ${blockchain.fullName}")
                    throw UnsupportedOperationException()
                }
            }
        }
    }
}

class TokenNetworkValidator : CustomTokenValidator<Blockchain> {
    override fun validate(data: Blockchain?): AddCustomTokenError? {
        return when (data) {
            null, Blockchain.Unknown -> AddCustomTokenError.NetworkIsNotSelected
            else -> null
        }
    }
}

class TokenNameValidator : CustomTokenValidator<String> {
    override fun validate(data: String?): AddCustomTokenError? = StringIsNotEmptyValidator().validate(data)
}

class TokenSymbolValidator : CustomTokenValidator<String> {
    override fun validate(data: String?): AddCustomTokenError? = StringIsNotEmptyValidator().validate(data)
}

class TokenDecimalsValidator : CustomTokenValidator<String> {
    override fun validate(data: String?): AddCustomTokenError? {
        val decimal = data?.toIntOrNull() ?: return AddCustomTokenError.FieldIsEmpty

        return if (decimal > INVALID_DECIMALS_COUNT) AddCustomTokenError.InvalidDecimalsCount else null
    }

    private companion object {
        const val INVALID_DECIMALS_COUNT = 30
    }
}
