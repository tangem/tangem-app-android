package com.tangem.tangemtest.ucase.variants.personalize.converter

import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ItemsToModel
import com.tangem.tangemtest._arch.structure.abstraction.ModelConverter
import com.tangem.tangemtest._arch.structure.abstraction.ModelToItems
import com.tangem.tangemtest.ucase.variants.personalize.converter.fromTo.ItemsToPersonalizeConfig
import com.tangem.tangemtest.ucase.variants.personalize.converter.fromTo.PersonalizeConfigToItem
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig

/**
[REDACTED_AUTHOR]
 */
class PersonalizeConfigConverter : ModelConverter<PersonalizeConfig> {

    private val toModel: ItemsToModel<PersonalizeConfig> = ItemsToPersonalizeConfig()
    private val toItems: ModelToItems<PersonalizeConfig> = PersonalizeConfigToItem()

    override fun convert(from: List<Item>, default: PersonalizeConfig): PersonalizeConfig {
        return toModel.convert(from, default)
    }

    override fun convert(from: PersonalizeConfig): List<Item> {
        return toItems.convert(from)
    }
}