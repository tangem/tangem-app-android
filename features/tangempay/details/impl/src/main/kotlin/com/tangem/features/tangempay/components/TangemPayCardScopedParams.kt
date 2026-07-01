package com.tangem.features.tangempay.components

import com.tangem.domain.models.account.AccountStatus

/**
 * Params for sub-screens that operate on one explicit card (e.g. rename) instead of implicitly
 * resolving the first card. Carries the [cardId] selected on the card page.
 */
internal data class TangemPayCardScopedParams(
    val initialStatus: AccountStatus.Payment,
    val cardId: String,
)