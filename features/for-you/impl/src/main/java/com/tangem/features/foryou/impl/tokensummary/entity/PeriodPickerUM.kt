package com.tangem.features.foryou.impl.tokensummary.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM

@Immutable
internal sealed interface PeriodPickerUM {

    data class Content(val picker: TangemSegmentedPickerUM) : PeriodPickerUM

    data object Loading : PeriodPickerUM

    data object Empty : PeriodPickerUM
}