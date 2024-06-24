package com.tangem.features.staking.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.features.staking.impl.presentation.StakingFragment

internal class DefaultStakingRouter : InnerStakingRouter {

    override fun getEntryFragment(): Fragment = StakingFragment.create()
}