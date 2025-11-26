package com.tangem.domain.transaction.models

/**
 * DTO representing the type of a transaction event on tangem backend to send
 * info about transaction happens and its hash
 */
enum class EventTransactionTypeDto {
    DEPOSIT,
    WITHDRAW,
    SEND,
}