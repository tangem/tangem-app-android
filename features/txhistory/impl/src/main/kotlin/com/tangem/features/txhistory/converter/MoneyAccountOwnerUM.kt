package com.tangem.features.txhistory.converter

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.txhistory.impl.R

internal val moneyAccountOwnerName: TextReference =
    resourceReference(R.string.tangempay_unified_onboarding_money_account_header)

internal val moneyAccountOwnerIcon: CryptoPortfolioIcon =
    CryptoPortfolioIcon.ofCustomAccount(value = AccountIconUM.Money.icon, color = AccountIconUM.Money.color)