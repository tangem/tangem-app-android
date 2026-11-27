package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.grow.datasource.polymarket.models.PolymarketTeamDto
import com.tangem.utils.converter.Converter

internal object PolymarketTeamConverter : Converter<PolymarketTeamDto, PolymarketTeam> {
    override fun convert(dto: PolymarketTeamDto): PolymarketTeam = PolymarketTeam(
        name = dto.name,
        abbreviation = dto.abbreviation,
        alias = dto.alias,
        logoUrl = dto.logo,
        ordering = dto.ordering,
    )
}