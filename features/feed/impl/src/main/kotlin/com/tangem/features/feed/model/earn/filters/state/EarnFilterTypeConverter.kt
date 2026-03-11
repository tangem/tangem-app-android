package com.tangem.features.feed.model.earn.filters.state

import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.utils.converter.Converter

internal class EarnFilterTypeConverter : Converter<EarnFilterType, EarnFilterTypeUM> {
    override fun convert(value: EarnFilterType): EarnFilterTypeUM {
        return when (value) {
            EarnFilterType.ALL -> EarnFilterTypeUM.All
            EarnFilterType.STAKING -> EarnFilterTypeUM.Staking
            EarnFilterType.YIELD -> EarnFilterTypeUM.YieldMode
        }
    }
}