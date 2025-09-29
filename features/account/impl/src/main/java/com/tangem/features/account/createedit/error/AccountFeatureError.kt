package com.tangem.features.account.createedit.error

import com.tangem.core.error.UniversalError
import com.tangem.domain.account.usecase.AddCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.GetUnoccupiedAccountIndexUseCase

sealed interface AccountFeatureError : UniversalError {

    val subsystemCode: String
    val specificErrorCode: String

    override val errorCode: Int
        get() = "108$subsystemCode$specificErrorCode".toInt()

    sealed interface CreateAccount : AccountFeatureError {

        override val subsystemCode: String get() = "001"

        data class UnableToGetDerivationIndex(val cause: GetUnoccupiedAccountIndexUseCase.Error) : CreateAccount {
            override val specificErrorCode: String = "001"
        }

        data class FailedToCreateAccount(val cause: AddCryptoPortfolioUseCase.Error) : CreateAccount {
            override val specificErrorCode: String = "002"
        }
    }

    sealed interface EditAccount : AccountFeatureError {

        override val subsystemCode: String get() = "002"

        data object RequiredCryptoPortfolio : EditAccount {
            override val specificErrorCode: String = "001"
        }
    }
}