package com.tangem.data.account.converter

import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.utils.converter.Converter

/**
 * Converts a [CryptoPortfolioIconConverter.DataModel] to a [CryptoPortfolioIcon]
 *
[REDACTED_AUTHOR]
 */
internal object CryptoPortfolioIconConverter : Converter<CryptoPortfolioIconConverter.DataModel, CryptoPortfolioIcon> {

    override fun convert(value: DataModel): CryptoPortfolioIcon {
        return CryptoPortfolioIcon.ofCustomAccount(
            value = CryptoPortfolioIcon.Icon.valueOf(value.icon),
            color = CryptoPortfolioIcon.Color.valueOf(value.color),
        )
    }

    data class DataModel(val icon: String, val color: String)
}