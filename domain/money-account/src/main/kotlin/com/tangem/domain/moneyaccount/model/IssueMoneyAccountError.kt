package com.tangem.domain.moneyaccount.model

sealed interface IssueMoneyAccountError {
    data object UserCancelled : IssueMoneyAccountError
    data object Failed : IssueMoneyAccountError
}