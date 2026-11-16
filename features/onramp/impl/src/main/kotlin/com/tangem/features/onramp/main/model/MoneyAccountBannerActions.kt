package com.tangem.features.onramp.main.model

import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.features.marketing.api.MoneyAccountBannerAction

internal fun MoneyAccountStatusValue.toBannerAction(): MoneyAccountBannerAction? = when (this) {
    is MoneyAccountStatusValue.Account -> MoneyAccountBannerAction.CONTINUE
    MoneyAccountStatusValue.Empty,
    is MoneyAccountStatusValue.Onboarding,
    -> MoneyAccountBannerAction.ACTIVATE
    is MoneyAccountStatusValue.Error -> null
}