package com.tangem.features.collectibles.impl.machinedetails.model.converter

import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.features.collectibles.impl.R
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Builds the "Pack details" cards. A card whose value the machine does not carry is left out. */
internal object CollectiblesMachineDetailRowsConverter :
    Converter<CollectibleGachaMachine, ImmutableList<CollectiblesMachineDetailsUM.DetailUM>> {

    private const val CONTAINS_ID = "contains"
    private const val BUYBACK_ID = "buyback"

    override fun convert(value: CollectibleGachaMachine): ImmutableList<CollectiblesMachineDetailsUM.DetailUM> {
        return buildList {
            value.contains?.let { contains ->
                add(
                    CollectiblesMachineDetailsUM.DetailUM(
                        id = CONTAINS_ID,
                        label = resourceReference(R.string.collectibles_machine_detail_contains),
                        value = pluralReference(
                            id = R.plurals.collectibles_account_collection_cards_count,
                            count = contains,
                            formatArgs = wrappedList(contains),
                        ),
                    ),
                )
            }

            add(
                CollectiblesMachineDetailsUM.DetailUM(
                    id = BUYBACK_ID,
                    label = resourceReference(R.string.collectibles_machine_detail_instant_buyback),
                    value = resourceReference(
                        R.string.collectibles_machine_buyback_value,
                        wrappedList("${value.instantBuybackPercent}%"),
                    ),
                ),
            )
        }.toImmutableList()
    }
}