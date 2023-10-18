package com.tangem.features.send.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.send.impl.presentation.SendFragment

internal class DefaultSendRouter : SendRouter {
    override fun getEntryFragment(): Fragment = SendFragment.create()
}