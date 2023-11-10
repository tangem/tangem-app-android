package com.tangem.managetokens.presentation.router

import androidx.fragment.app.Fragment
import com.tangem.features.managetokens.navigation.ManageTokensRouter
import com.tangem.managetokens.ManageTokensFragment

internal class DefaultManageTokensRouter : ManageTokensRouter {
    override fun getEntryFragment(): Fragment = ManageTokensFragment()
}