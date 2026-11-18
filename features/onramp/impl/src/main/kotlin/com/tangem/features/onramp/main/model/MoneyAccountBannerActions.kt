package com.tangem.features.onramp.main.model

import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.features.marketing.api.MoneyAccountBannerAction

/**
 * `null` keeps the campaign's own button text. [MoneyAccountStatusValue.Empty] is also what the status fetcher emits
 * when the status could not be loaded ([REDACTED_TASK_KEY]), so it does not prove the user has no account yet.
 */
internal fun MoneyAccountStatusValue.toBannerAction(): MoneyAccountBannerAction? = when (this) {
    is MoneyAccountStatusValue.Account -> MoneyAccountBannerAction.CONTINUE
    is MoneyAccountStatusValue.Onboarding -> MoneyAccountBannerAction.ACTIVATE
    MoneyAccountStatusValue.Empty,
    is MoneyAccountStatusValue.Error,
    -> null
}