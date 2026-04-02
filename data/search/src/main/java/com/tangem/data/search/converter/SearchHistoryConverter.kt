package com.tangem.data.search.converter

import com.tangem.data.search.model.RecentTokenDTO
import com.tangem.data.search.model.TextHintDTO
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.search.model.RecentSearchToken
import com.tangem.domain.search.model.SearchTextHint
import com.tangem.utils.converter.Converter

internal class TextHintDTOToSearchTextHintConverter : Converter<TextHintDTO, SearchTextHint> {
    override fun convert(value: TextHintDTO): SearchTextHint {
        return SearchTextHint(
            text = value.text,
            timestamp = value.timestamp,
        )
    }
}

internal class RecentTokenDTOToRecentSearchTokenConverter : Converter<RecentTokenDTO, RecentSearchToken> {
    override fun convert(value: RecentTokenDTO): RecentSearchToken {
        return RecentSearchToken(
            id = CryptoCurrency.RawID(value.id),
            name = value.name,
            symbol = value.symbol,
            imageUrl = value.imageUrl,
            timestamp = value.timestamp,
        )
    }
}

internal class RecentSearchTokenToRecentTokenDTOConverter : Converter<RecentSearchToken, RecentTokenDTO> {
    override fun convert(value: RecentSearchToken): RecentTokenDTO {
        return RecentTokenDTO(
            id = value.id.value,
            name = value.name,
            symbol = value.symbol,
            imageUrl = value.imageUrl,
            timestamp = value.timestamp,
        )
    }
}