package com.tangem.tap.common.extensions

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.tangem.common.extensions.VoidCallback
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import com.tangem.tap.features.details.ui.DetailsConfirmFragment
import com.tangem.tap.features.details.ui.DetailsFragment
import com.tangem.tap.features.details.ui.DetailsSecurityFragment
import com.tangem.tap.features.details.ui.twins.CreateTwinWalletFragment
import com.tangem.tap.features.details.ui.twins.TwinWalletWarningFragment
import com.tangem.tap.features.details.ui.walletconnect.QrScanFragment
import com.tangem.tap.features.details.ui.walletconnect.WalletConnectSessionsFragment
import com.tangem.tap.features.disclaimer.ui.DisclaimerFragment
import com.tangem.tap.features.home.HomeFragment
import com.tangem.tap.features.onboarding.products.note.OnboardingNoteFragment
import com.tangem.tap.features.onboarding.products.old.OnboardingOtherCardsFragment
import com.tangem.tap.features.onboarding.products.wallet.OnboardingWalletFragment
import com.tangem.tap.features.send.ui.SendFragment
import com.tangem.tap.features.tokens.ui.AddTokensFragment
import com.tangem.tap.features.wallet.ui.OnboardingTwinsFragment
import com.tangem.tap.features.wallet.ui.WalletDetailsFragment
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.wallet.R

fun FragmentActivity.openFragment(
        screen: AppScreen,
        addToBackstack: Boolean,
        fgShareTransition: FragmentShareTransition? = null
) {
    val transaction = this.supportFragmentManager.beginTransaction()
    val fragment = fragmentFactory(screen)
    fgShareTransition?.apply {
        fragment.sharedElementEnterTransition = enterTransitionSet
        fragment.sharedElementReturnTransition = exitTransitionSet
        transaction.setReorderingAllowed(true)
        shareElements.forEach { shareElement ->
            shareElement.wView.get()?.let { view ->
                transaction.addSharedElement(view, shareElement.name)
            }
        }
    }
    transaction.replace(R.id.fragment_container, fragment, screen.name)
    if (addToBackstack && screen != AppScreen.Home) transaction.addToBackStack(null)
    transaction.commitAllowingStateLoss()
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

fun FragmentActivity.addOnBackPressedDispatcher(
        isEnabled: Boolean = true,
        onBackPressed: VoidCallback
): OnBackPressedCallback = (object : OnBackPressedCallback(isEnabled) {
    override fun handleOnBackPressed() {
        onBackPressed()
    }
}).also { this.onBackPressedDispatcher.addCallback(it) }

private fun fragmentFactory(screen: AppScreen): Fragment {
    return when (screen) {
        AppScreen.Home -> HomeFragment()
        AppScreen.OnboardingNote -> OnboardingNoteFragment()
        AppScreen.OnboardingWallet -> OnboardingWalletFragment()
        AppScreen.OnboardingTwins -> OnboardingTwinsFragment()
        AppScreen.OnboardingOther -> OnboardingOtherCardsFragment()
        AppScreen.Wallet -> WalletFragment()
        AppScreen.Send -> SendFragment()
        AppScreen.Details -> DetailsFragment()
        AppScreen.DetailsConfirm -> DetailsConfirmFragment()
        AppScreen.DetailsSecurity -> DetailsSecurityFragment()
        AppScreen.Disclaimer -> DisclaimerFragment()
        AppScreen.CreateTwinWalletWarning -> TwinWalletWarningFragment()
        AppScreen.CreateTwinWallet -> CreateTwinWalletFragment()
        AppScreen.AddTokens -> AddTokensFragment()
        AppScreen.WalletDetails -> WalletDetailsFragment()
        AppScreen.WalletConnectSessions -> WalletConnectSessionsFragment()
        AppScreen.QrScan -> QrScanFragment()
    }
}