package com.tangem.features.staking.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.core.navigation.ReduxNavController

internal class DefaultStakingRouter(
    private val reduxNavController: ReduxNavController,
) : InnerStakingRouter {

    override fun getEntryFragment(): Fragment = TODO()

}
