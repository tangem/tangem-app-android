package com.tangem.features.account.createedit.error

import com.tangem.core.error.UniversalError

sealed interface AccountFeatureError : UniversalError {

    val subsystemCode: String
    val specificErrorCode: String

    override val errorCode: Int
        get() = "108$subsystemCode$specificErrorCode".toInt()

    sealed interface CreateAccount : AccountFeatureError {

        override val subsystemCode: String get() = "001"

        data object UnableToGetDerivationIndex : CreateAccount {
            override val specificErrorCode: String = "001"
        }
    }

    sealed interface EditAccount : AccountFeatureError {

        override val subsystemCode: String get() = "002"

        data object RequiredCryptoPortfolio : EditAccount {
            override val specificErrorCode: String = "001"
        }
    }
}