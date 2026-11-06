package com.tangem.features.txhistory.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.txhistory.impl.R

internal val moneyAccountOwnerName: TextReference =
    resourceReference(R.string.money_account_main_name)

internal val moneyAccountOwnerIcon: CryptoPortfolioIcon =
    CryptoPortfolioIcon.ofCustomAccount(
        value = CryptoPortfolioIcon.Icon.Safe,
        color = CryptoPortfolioIcon.Color.VitalGreen,
    )