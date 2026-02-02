package com.tangem.common.ui.account

import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.utils.converter.TwoWayConverter

object CryptoPortfolioIconConverter : TwoWayConverter<CryptoPortfolioIcon, AccountIconUM.CryptoPortfolio> {
    override fun convert(value: CryptoPortfolioIcon): AccountIconUM.CryptoPortfolio {
        return AccountIconUM.CryptoPortfolio(value = value.value, color = value.color)
    }

    override fun convertBack(value: AccountIconUM.CryptoPortfolio): CryptoPortfolioIcon {
        return CryptoPortfolioIcon.ofCustomAccount(value = value.value, color = value.color)
    }
}