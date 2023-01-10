package com.tangem.datasource.api.paymentology.models.response

enum class KYCStatus {
    NOT_STARTED,
    STARTED,
    WAITING_FOR_APPROVAL,
    CORRECTION_REQUESTED,
    REJECTED,
    APPROVED,
}
