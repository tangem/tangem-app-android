package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.grow.datasource.collectibles.models.response.model.MachineNftDTO
import com.tangem.utils.converter.Converter

internal object MachineNftConverter : Converter<MachineNftDTO, CollectibleGachaMachineNft> {

    /** The backend sends the appraised value in cents, the domain carries whole currency units. */
    private const val CENTS_SCALE = 2

    override fun convert(value: MachineNftDTO): CollectibleGachaMachineNft = CollectibleGachaMachineNft(
        id = value.id,
        nftAddress = value.nftAddress,
        name = value.name,
        description = value.description,
        rarity = value.rarity?.toRarity(),
        grade = value.toGrade(),
        imageUrl = value.image,
        insuredValue = value.insuredValue?.movePointLeft(CENTS_SCALE),
    )

    private fun String.toRarity(): CollectibleRarity? =
        CollectibleRarity.entries.firstOrNull { it.name.equals(other = this, ignoreCase = true) }

    private fun MachineNftDTO.toGrade(): CollectibleGachaMachineNft.Grade? {
        val company = gradingCompany?.takeIf(String::isNotBlank) ?: return null
        val grade = gradeNum?.takeIf(String::isNotBlank) ?: return null

        return CollectibleGachaMachineNft.Grade(company = company, value = grade)
    }
}