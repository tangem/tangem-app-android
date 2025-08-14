package com.tangem.features.account.common

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
}

fun CryptoPortfolioIcon.toUM() = CryptoPortfolioIconUM(this)
fun CryptoPortfolioIconUM.toDomain() = CryptoPortfolioIcon.ofCustomAccount(this.value, this.color)