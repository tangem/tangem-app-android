package com.tangem.tap.common.extensions

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.tangem.common.extensions.VoidCallback
import com.tangem.feature.referral.ReferralFragment
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import com.tangem.tap.features.details.ui.appsettings.AppSettingsFragment
import com.tangem.tap.features.details.ui.cardsettings.CardSettingsFragment
import com.tangem.tap.features.details.ui.details.DetailsFragment
import com.tangem.tap.features.details.ui.resetcard.ResetCardFragment
import com.tangem.tap.features.details.ui.securitymode.SecurityModeFragment
import com.tangem.tap.features.details.ui.walletconnect.QrScanFragment
import com.tangem.tap.features.details.ui.walletconnect.WalletConnectFragment
import com.tangem.tap.features.disclaimer.ui.DisclaimerFragment
import com.tangem.tap.features.home.HomeFragment
import com.tangem.tap.features.onboarding.products.note.OnboardingNoteFragment
import com.tangem.tap.features.onboarding.products.otherCards.OnboardingOtherCardsFragment
import com.tangem.tap.features.onboarding.products.twins.ui.TwinsCardsFragment
import com.tangem.tap.features.onboarding.products.wallet.ui.OnboardingWalletFragment
import com.tangem.tap.features.saveWallet.ui.SaveWalletBottomSheetFragment
import com.tangem.tap.features.send.ui.SendFragment
import com.tangem.tap.features.shop.ui.ShopFragment
import com.tangem.tap.features.tokens.addCustomToken.AddCustomTokenFragment
import com.tangem.tap.features.tokens.ui.AddTokensFragment
import com.tangem.tap.features.wallet.ui.WalletDetailsFragment
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.walletSelector.ui.WalletSelectorBottomSheetFragment
import com.tangem.tap.features.welcome.ui.WelcomeFragment
import com.tangem.wallet.R
import timber.log.Timber

fun FragmentActivity.openFragment(
    screen: AppScreen,
    addToBackstack: Boolean,
    fgShareTransition: FragmentShareTransition? = null,
) {
    val transaction = this.supportFragmentManager.beginTransaction()
    val fragment = fragmentFactory(screen)
    fgShareTransition?.apply {
        fragment.sharedElementEnterTransition = enterTransitionSet
        fragment.sharedElementReturnTransition = exitTransitionSet
        transaction.setReorderingAllowed(true)
        shareElements.forEach { shareElement ->
            shareElement.wView.get()?.let { view ->
                transaction.addSharedElement(view, shareElement.elementName)
            }
        }
    }
    if (screen.isDialogFragment) {
        (fragment as DialogFragment).show(transaction, screen.name)
        if (addToBackstack) {
            transaction.addToBackStack(screen.name)
        }
    } else {
        transaction.replace(R.id.fragment_container, fragment, screen.name)
        if (addToBackstack) {
            transaction.addToBackStack(screen.name)
        }
        transaction.commitAllowingStateLoss()
    }
}

fun FragmentActivity.popBackTo(screen: AppScreen?, inclusive: Boolean = false) {
    val inclusiveFlag = if (inclusive) FragmentManager.POP_BACK_STACK_INCLUSIVE else 0
    try {
        this.supportFragmentManager.popBackStack(screen?.name, inclusiveFlag)
    } catch (e: IllegalStateException) {
        Timber.e(e)
    }
}

fun FragmentActivity.getPreviousScreen(): AppScreen? {
    val indexOfLastFragment = if (this.supportFragmentManager.backStackEntryCount > 0) {
        this.supportFragmentManager.backStackEntryCount - 1
    } else {
        0
    }
    val tag = if (indexOfLastFragment < this.supportFragmentManager.backStackEntryCount)
        this.supportFragmentManager.getBackStackEntryAt(indexOfLastFragment).name
    else null
    return tag?.let { AppScreen.valueOf(tag) }
}

fun FragmentActivity.addOnBackPressedDispatcher(
    isEnabled: Boolean = true,
    onBackPressed: VoidCallback,
): OnBackPressedCallback = (object : OnBackPressedCallback(isEnabled) {
    override fun handleOnBackPressed() {
        onBackPressed()
    }
}).also { this.onBackPressedDispatcher.addCallback(it) }

private fun fragmentFactory(screen: AppScreen): Fragment {
    return when (screen) {
        AppScreen.Home -> HomeFragment()
        AppScreen.Shop -> ShopFragment()
        AppScreen.OnboardingNote -> OnboardingNoteFragment()
        AppScreen.OnboardingWallet -> OnboardingWalletFragment()
        AppScreen.OnboardingTwins -> TwinsCardsFragment()
        AppScreen.OnboardingOther -> OnboardingOtherCardsFragment()
        AppScreen.Wallet -> WalletFragment()
        AppScreen.Send -> SendFragment()
        AppScreen.Details -> DetailsFragment()
        AppScreen.DetailsSecurity -> SecurityModeFragment()
        AppScreen.CardSettings -> CardSettingsFragment()
        AppScreen.AppSettings -> AppSettingsFragment()
        AppScreen.ResetToFactory -> ResetCardFragment()
        AppScreen.Disclaimer -> DisclaimerFragment()
        AppScreen.AddTokens -> AddTokensFragment()
        AppScreen.AddCustomToken -> AddCustomTokenFragment()
        AppScreen.WalletDetails -> WalletDetailsFragment()
        AppScreen.WalletConnectSessions -> WalletConnectFragment()
        AppScreen.QrScan -> QrScanFragment()
        AppScreen.ReferralProgram -> ReferralFragment()
        AppScreen.Welcome -> WelcomeFragment()
        AppScreen.SaveWallet -> SaveWalletBottomSheetFragment()
        AppScreen.WalletSelector -> WalletSelectorBottomSheetFragment()
    }
}
