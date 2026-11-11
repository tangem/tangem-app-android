package com.tangem.domain.tangempay.model

import com.tangem.domain.tangempay.error.TangemPayApiError

sealed interface IssueAdditionalCardResult {

    val order: Order

    data class Created(override val order: Order) : IssueAdditionalCardResult

    data class Resumed(override val order: Order) : IssueAdditionalCardResult
}

sealed interface IssueAdditionalCardError {

    data object OfferNotAvailable : IssueAdditionalCardError

    data object NoCampaignOffer : IssueAdditionalCardError

    data class Other(val cause: TangemPayApiError) : IssueAdditionalCardError

    fun isOutcomeUnknown() = this is Other && cause.isOutcomeUnknown()
}