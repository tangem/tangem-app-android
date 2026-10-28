package com.tangem.features.foryou.impl.tokensummary.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface PeriodPickerUM {

    data class Content(val items: ImmutableList<TangemSegmentedControl.Item>) : PeriodPickerUM

    data object Loading : PeriodPickerUM

    data object Empty : PeriodPickerUM
}