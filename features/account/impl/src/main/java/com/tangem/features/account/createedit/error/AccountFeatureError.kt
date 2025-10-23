package com.tangem.features.account.createedit.error

import com.tangem.core.error.UniversalError
import com.tangem.domain.account.status.usecase.RecoverCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.AddCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.GetUnoccupiedAccountIndexUseCase
import com.tangem.domain.account.usecase.UpdateCryptoPortfolioUseCase

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

        data class FailedToEditAccount(val cause: UpdateCryptoPortfolioUseCase.Error) : EditAccount {
            override val specificErrorCode: String = "001"
        }
    }

    sealed interface ArchivedAccountList : AccountFeatureError {

        override val subsystemCode: String get() = "003"

        data class FailedToRecoverAccount(val cause: RecoverCryptoPortfolioUseCase.Error) : ArchivedAccountList {
            override val specificErrorCode: String = "001"
        }
    }
}