package com.tangem.common.ui.account

import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.CryptoPortfolioIcon.Color
import com.tangem.domain.models.account.CryptoPortfolioIcon.Icon

data class CryptoPortfolioIconUM(
    val value: Icon,
    val color: Color,
) {
    constructor(domainModel: CryptoPortfolioIcon) : this(
        value = domainModel.value,
        color = domainModel.color,
    )

    companion object {
        fun random(letterIcon: Boolean = false) = CryptoPortfolioIconUM(
            value = if (letterIcon) Icon.Letter else Icon.entries.random(),
            color = Color.entries.random(),
        )
    }
}