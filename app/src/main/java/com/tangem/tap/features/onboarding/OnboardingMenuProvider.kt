package com.tangem.tap.features.onboarding

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.feedback.SupportInfo
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 20.10.2022.
 */
class OnboardingMenuProvider : MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_onboarding, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.menu_item_chat_support -> {
            Analytics.send(Onboarding.ButtonChat())
            store.dispatch(GlobalAction.OpenChat(SupportInfo()))
            true
        }
        else -> false
    }
}
