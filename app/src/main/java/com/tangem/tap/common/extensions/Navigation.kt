package com.tangem.tap.common.extensions

import android.os.Bundle
import androidx.fragment.app.*
import com.tangem.feature.referral.ReferralFragment
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import com.tangem.tap.features.customtoken.legacy.AddCustomTokenFragment
import com.tangem.tap.features.details.ui.appsettings.AppSettingsFragment
import com.tangem.tap.features.details.ui.cardsettings.CardSettingsFragment
import com.tangem.tap.features.details.ui.cardsettings.coderecovery.AccessCodeRecoveryFragment
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
import com.tangem.tap.features.tokens.impl.presentation.TokensListFragment
import com.tangem.tap.features.wallet.ui.WalletDetailsFragment
import com.tangem.tap.features.wallet.ui.WalletFragment
import com.tangem.tap.features.walletSelector.ui.WalletSelectorBottomSheetFragment
import com.tangem.tap.features.welcome.ui.WelcomeFragment
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.wallet.R
import timber.log.Timber
import com.tangem.tap.features.customtoken.impl.presentation.AddCustomTokenFragment as RedesignedAddCustomTokenFragment

fun FragmentActivity.openFragment(
    screen: AppScreen,
    addToBackstack: Boolean,
    bundle: Bundle? = null,
    fgShareTransition: FragmentShareTransition? = null,
) {
    val transaction = supportFragmentManager.beginTransaction().apply {
        setReorderingAllowed(true)
    }

    val fragment = fragmentFactory(screen).apply {
        arguments = bundle

        if (fgShareTransition != null) {
            sharedElementEnterTransition = fgShareTransition.enterTransitionSet
            sharedElementReturnTransition = fgShareTransition.exitTransitionSet
            fgShareTransition.shareElements.forEach { shareElement ->
                shareElement.wView.get()?.let { view ->
                    transaction.addSharedElement(view, shareElement.elementName)
                }
            }
        }
    }

    if (screen.isDialogFragment) {
        val dialogFragment = requireNotNull(fragment as? DialogFragment) {
            "If screen.isDialogFragment == true then fragment must be a DialogFragment"
        }

        dialogFragment.showAllowingStateLoss(
            fragmentManager = supportFragmentManager,
            baseTransaction = transaction,
            tag = screen.name,
            addToBackstack = addToBackstack,
        )
    } else {
        transaction.replace(R.id.fragment_container, fragment, screen.name)
        if (addToBackstack) {
            transaction.addToBackStack(screen.name)
        }
        transaction.commitAllowingStateLoss()
    }
}

private fun DialogFragment.showAllowingStateLoss(
    fragmentManager: FragmentManager,
    baseTransaction: FragmentTransaction,
    tag: String,
    addToBackstack: Boolean,
) {
    runCatching {
        if (addToBackstack) baseTransaction.addToBackStack(tag)
        show(baseTransaction, tag)
    }
        .onFailure { throwable ->
            if (throwable is IllegalStateException) {
                val transaction = fragmentManager.beginTransaction()
                transaction.add(this, tag)
                if (addToBackstack) transaction.addToBackStack(tag)

                transaction.commitAllowingStateLoss()
            } else {
                Timber.e(throwable)
            }
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
    val tag = if (indexOfLastFragment < this.supportFragmentManager.backStackEntryCount) {
        this.supportFragmentManager.getBackStackEntryAt(indexOfLastFragment).name
    } else {
        null
    }
    return tag?.let { AppScreen.valueOf(tag) }
}

@Suppress("ComplexMethod")
private fun fragmentFactory(screen: AppScreen): Fragment {
    return when (screen) {
        AppScreen.Home -> HomeFragment()
        AppScreen.Shop -> ShopFragment()
        AppScreen.OnboardingNote -> OnboardingNoteFragment()
        AppScreen.OnboardingWallet -> OnboardingWalletFragment()
        AppScreen.OnboardingTwins -> TwinsCardsFragment()
        AppScreen.OnboardingOther -> OnboardingOtherCardsFragment()
        AppScreen.Wallet -> {
            val featureToggles = store.state.daggerGraphState.get(
                getDependency = DaggerGraphState::walletFeatureToggles,
            )
            if (featureToggles.isRedesignedScreenEnabled) {
                store.state.daggerGraphState
                    .get(getDependency = DaggerGraphState::walletRouter)
                    .getEntryFragment()
            } else {
                WalletFragment()
            }
        }
        AppScreen.Send -> SendFragment()
        AppScreen.Details -> DetailsFragment()
        AppScreen.DetailsSecurity -> SecurityModeFragment()
        AppScreen.CardSettings -> CardSettingsFragment()
        AppScreen.AppSettings -> AppSettingsFragment()
        AppScreen.ResetToFactory -> ResetCardFragment()
        AppScreen.AccessCodeRecovery -> AccessCodeRecoveryFragment()
        AppScreen.Disclaimer -> DisclaimerFragment()
        AppScreen.AddTokens -> TokensListFragment()

        AppScreen.AddCustomToken -> {
            val featureToggles = store.state.daggerGraphState.get(
                getDependency = DaggerGraphState::customTokenFeatureToggles,
            )
            if (featureToggles.isRedesignedScreenEnabled) {
                RedesignedAddCustomTokenFragment()
            } else {
                AddCustomTokenFragment()
            }
        }

        AppScreen.WalletDetails -> {
            val featureToggles = store.state.daggerGraphState.get(
                getDependency = DaggerGraphState::tokenDetailsFeatureToggles,
            )
            if (featureToggles.isRedesignedScreenEnabled) {
                store.state.daggerGraphState
                    .get(getDependency = DaggerGraphState::tokenDetailsRouter)
                    .getEntryFragment()
            } else {
                WalletDetailsFragment()
            }
        }
        AppScreen.WalletConnectSessions -> WalletConnectFragment()
        AppScreen.QrScan -> QrScanFragment()
        AppScreen.ReferralProgram -> ReferralFragment()
        AppScreen.Swap -> SwapFragment()
        AppScreen.Welcome -> WelcomeFragment()
        AppScreen.SaveWallet -> SaveWalletBottomSheetFragment()
        AppScreen.WalletSelector -> WalletSelectorBottomSheetFragment()
    }
}