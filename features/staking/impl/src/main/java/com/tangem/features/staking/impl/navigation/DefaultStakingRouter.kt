package com.tangem.features.staking.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.staking.impl.presentation.StakingFragment

internal class DefaultStakingRouter(
    private val urlOpener: UrlOpener,
) : InnerStakingRouter {
    override fun getEntryFragment(): Fragment = StakingFragment.create()

    override fun openUrl(url: String) {
        urlOpener.openUrl(url)
    }
}
