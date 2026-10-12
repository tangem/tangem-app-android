package com.tangem.data.collectibles.converter

import com.tangem.domain.collectibles.models.CollectibleGachaMachineCategories
import com.tangem.grow.datasource.collectibles.models.response.MachineCategoriesResponse
import com.tangem.utils.converter.Converter

internal object MachineCategoriesConverter : Converter<MachineCategoriesResponse, CollectibleGachaMachineCategories> {

    override fun convert(value: MachineCategoriesResponse): CollectibleGachaMachineCategories =
        CollectibleGachaMachineCategories(
            categories = value.categories.map { category ->
                CollectibleGachaMachineCategories.MachineCategory(
                    id = category.id,
                    name = category.name,
                    machinesQuantity = category.machinesQuantity,
                )
            },
            totalMachinesQuantity = value.totalMachinesQuantity,
        )
}