package com.tangem.tap.common.extensions

import android.os.Bundle
import androidx.fragment.app.*
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.FragmentShareTransition
import com.tangem.feature.referral.ReferralFragment
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.tap.features.customtoken.impl.presentation.AddCustomTokenFragment
import com.tangem.tap.features.details.ui.appcurrency.AppCurrencySelectorFragment
import com.tangem.tap.features.details.ui.appsettings.AppSettingsFragment
import com.tangem.tap.features.details.ui.cardsettings.CardSettingsFragment
import com.tangem.tap.features.details.ui.cardsettings.coderecovery.AccessCodeRecoveryFragment
import com.tangem.tap.features.details.ui.details.DetailsFragment
import com.tangem.tap.features.details.ui.resetcard.ResetCardFragment
import com.tangem.tap.features.details.ui.securitymode.SecurityModeFragment
import com.tangem.tap.features.details.ui.walletconnect.WalletConnectFragment
import com.tangem.tap.features.disclaimer.ui.DisclaimerFragment
import com.tangem.tap.features.home.HomeFragment
import com.tangem.tap.features.main.ui.ModalNotificationBottomSheetFragment
import com.tangem.tap.features.onboarding.products.note.OnboardingNoteFragment
import com.tangem.tap.features.onboarding.products.otherCards.OnboardingOtherCardsFragment
import com.tangem.tap.features.onboarding.products.twins.ui.OnboardingTwinsFragment
import com.tangem.tap.features.onboarding.products.wallet.ui.OnboardingWalletFragment
import com.tangem.tap.features.saveWallet.ui.SaveWalletBottomSheetFragment
import com.tangem.tap.features.send.ui.SendFragment
import com.tangem.tap.features.tokens.impl.presentation.TokensListFragment
import com.tangem.tap.features.welcome.ui.WelcomeFragment
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.wallet.R
import timber.log.Timber

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

    if (screen.isDialogFragment && fragment is DialogFragment) {
        fragment.showAllowingStateLoss(
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

@Suppress("ComplexMethod", "LongMethod")
private fun fragmentFactory(screen: AppScreen): Fragment {
    return when (screen) {
        AppScreen.Home -> HomeFragment()
        AppScreen.OnboardingNote -> OnboardingNoteFragment()
        AppScreen.OnboardingWallet -> OnboardingWalletFragment()
        AppScreen.OnboardingTwins -> OnboardingTwinsFragment()
        AppScreen.OnboardingOther -> OnboardingOtherCardsFragment()
        AppScreen.Wallet -> {
            store.inject(getDependency = DaggerGraphState::walletRouter).getEntryFragment()
        }
        AppScreen.Send -> {
            val featureToggles = store.inject(getDependency = DaggerGraphState::sendFeatureToggles)

            if (featureToggles.isRedesignedSendEnabled) {
                store.inject(getDependency = DaggerGraphState::sendRouter).getEntryFragment()
            } else {
                SendFragment()
            }
        }
        AppScreen.Details -> DetailsFragment()
        AppScreen.DetailsSecurity -> SecurityModeFragment()
        AppScreen.CardSettings -> CardSettingsFragment()
        AppScreen.AppSettings -> AppSettingsFragment()
        AppScreen.ResetToFactory -> ResetCardFragment()
        AppScreen.AccessCodeRecovery -> AccessCodeRecoveryFragment()
        AppScreen.Disclaimer -> DisclaimerFragment()
        AppScreen.ManageTokens -> TokensListFragment()
        AppScreen.AddCustomToken -> AddCustomTokenFragment()
        AppScreen.WalletDetails -> {
            store.inject(getDependency = DaggerGraphState::tokenDetailsRouter).getEntryFragment()
        }
        AppScreen.WalletConnectSessions -> WalletConnectFragment()
        AppScreen.QrScanning -> {
            store.inject(getDependency = DaggerGraphState::qrScanningRouter).getEntryFragment()
        }
        AppScreen.ReferralProgram -> ReferralFragment()
        AppScreen.Swap -> SwapFragment()
        AppScreen.Welcome -> WelcomeFragment()
        AppScreen.SaveWallet -> SaveWalletBottomSheetFragment()
        AppScreen.AppCurrencySelector -> AppCurrencySelectorFragment()
        AppScreen.ModalNotification -> ModalNotificationBottomSheetFragment()
    }
}