package com.tangem.tap.common.extensions

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.features.details.ui.DetailsFragment
import com.tangem.tap.features.home.HomeFragment
import com.tangem.tap.features.send.ui.SendFragment
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.wallet.R

fun FragmentActivity.openFragment(screen: AppScreen, addToBackStack: Boolean = true) {
    val transaction = this.supportFragmentManager.beginTransaction()
            .replace(
                    R.id.fragment_container,
                    fragmentFactory(screen),
                    screen.name
            )
    if (addToBackStack && screen != AppScreen.Home) transaction.addToBackStack(null)
    transaction.commit();
}

fun FragmentActivity.popBackTo(screen: AppScreen?, inclusive: Boolean = false) {
    val inclusiveFlag = if (inclusive) FragmentManager.POP_BACK_STACK_INCLUSIVE else 0
    this.supportFragmentManager.popBackStack(screen?.name, inclusiveFlag)
}

fun FragmentActivity.getPreviousScreen(): AppScreen? {
    val indexOfLastFragment = this.supportFragmentManager.backStackEntryCount - 1
    val tag = this.supportFragmentManager.getBackStackEntryAt(indexOfLastFragment).name
    return tag?.let { AppScreen.valueOf(tag) }
}

private fun fragmentFactory(screen: AppScreen): Fragment {
    return when (screen) {
        AppScreen.Home -> HomeFragment()
        AppScreen.Wallet -> WalletFragment()
        AppScreen.Send -> SendFragment()
        AppScreen.Details -> DetailsFragment()
    }
}