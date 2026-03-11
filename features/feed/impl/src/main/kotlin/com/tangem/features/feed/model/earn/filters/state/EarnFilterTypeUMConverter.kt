package com.tangem.features.feed.model.earn.filters.state

import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.utils.converter.Converter

internal class EarnFilterTypeUMConverter : Converter<EarnFilterTypeUM, EarnFilterType> {

    override fun convert(value: EarnFilterTypeUM): EarnFilterType {
        return when (value) {
            EarnFilterTypeUM.All -> EarnFilterType.ALL
            EarnFilterTypeUM.Staking -> EarnFilterType.STAKING
            EarnFilterTypeUM.YieldMode -> EarnFilterType.YIELD
        }
    }
}