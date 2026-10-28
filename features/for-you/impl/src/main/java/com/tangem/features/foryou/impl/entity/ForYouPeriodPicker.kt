package com.tangem.features.foryou.impl.entity

import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.features.foryou.model.ForYouPeriod
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun forYouPeriodPickerItems(
    selected: ForYouPeriod,
    onClick: (ForYouPeriod) -> Unit,
): ImmutableList<TangemSegmentedControl.Item> = ForYouPeriod.entries.map { period ->
    TangemSegmentedControl.Item(
        id = period.id,
        label = period.title,
        isSelected = period == selected,
        onClick = { onClick(period) },
    )
}.toImmutableList()

internal fun ImmutableList<TangemSegmentedControl.Item>.withSelected(
    period: ForYouPeriod,
): ImmutableList<TangemSegmentedControl.Item> = map { it.copy(isSelected = it.id == period.id) }.toImmutableList()