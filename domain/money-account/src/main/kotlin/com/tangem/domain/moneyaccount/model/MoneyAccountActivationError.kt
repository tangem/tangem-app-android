package com.tangem.domain.moneyaccount.model

sealed interface MoneyAccountActivationError {
    data object UserCancelled : MoneyAccountActivationError
    data object PersistenceFailed : MoneyAccountActivationError
    data object Unknown : MoneyAccountActivationError
}