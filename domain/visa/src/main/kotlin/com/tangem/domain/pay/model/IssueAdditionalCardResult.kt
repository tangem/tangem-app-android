package com.tangem.domain.pay.model

import com.tangem.domain.visa.error.VisaApiError

sealed interface IssueAdditionalCardResult {

    val order: Order

    data class Created(override val order: Order) : IssueAdditionalCardResult

    data class Resumed(override val order: Order) : IssueAdditionalCardResult
}

sealed interface IssueAdditionalCardError {

    data object OfferNotAvailable : IssueAdditionalCardError

    data object NoCampaignOffer : IssueAdditionalCardError

    data class Other(val cause: VisaApiError) : IssueAdditionalCardError
}