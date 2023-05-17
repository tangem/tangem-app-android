package com.tangem.tap.domain.scanCard.chains

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.services.Result
import com.tangem.core.analytics.Analytics
import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.twinsIsTwinned
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.common.extensions.addContext
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.onboarding.OnboardingSaltPayHelper
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsStep
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayActivationManagerFactory
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayExceptionHandler
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayAction
import kotlinx.coroutines.delay
import org.rekotlin.Store

/**
 * Handles the verification process to determine if the scanned card requires onboarding.
 *
 * Returns:
 * - [ScanChainException.OnboardingNeeded] if onboarding required for none SaltPay cards.
 * - [ScanChainException.CheckForSaltPayOnboardingCaseException] if unable to check for SaltPay onboarding case.
 * - [ScanChainException.PutSaltPayVisaCard] if scanned SaltPay card is not activated and onboarding required.
 *
 * @param store the [Store] that holds the state of the app.
 * @param tapWalletManager manager responsible for handling operations related to the Wallet.
 * @param preferencesStore data source for user preferences.
 *
 * @see Chain for more information about the Chain interface.
 */
class CheckForOnboardingChain(
    private val store: Store<AppState>,
    private val tapWalletManager: TapWalletManager,
    private val preferencesStore: PreferencesDataSource,
) : Chain<ScanCardException.ChainException, ScanResponse> {

    override suspend fun invoke(
        previousChainResult: ScanResponse,
    ): Either<ScanCardException.ChainException, ScanResponse> {
        val cardTypesResolver = previousChainResult.cardTypesResolver

        tapWalletManager.updateConfigManager(previousChainResult)
        store.dispatchOnMain(TwinCardsAction.IfTwinsPrepareState(previousChainResult))

        return when {
            cardTypesResolver.isSaltPayVisa() -> {
                val manager = SaltPayActivationManagerFactory(
                    blockchain = previousChainResult.cardTypesResolver.getBlockchain(),
                    card = previousChainResult.card,
                ).create()
                val result = OnboardingSaltPayHelper.isOnboardingCase(previousChainResult, manager)
                delay(timeMillis = 500)
                withMainContext {
                    when (result) {
                        is Result.Success -> {
                            Analytics.addContext(previousChainResult)
                            val isOnboardingCase = result.data
                            if (isOnboardingCase) {
                                store.dispatch(
                                    GlobalAction.Onboarding.Start(previousChainResult, canSkipBackup = false),
                                )
                                store.dispatch(OnboardingSaltPayAction.SetDependencies(manager))
                                store.dispatch(OnboardingSaltPayAction.Update(withAnalytics = false))
                                ScanChainException.OnboardingNeeded(AppScreen.OnboardingWallet).left()
                            } else {
                                delay(DELAY_SDK_DIALOG_CLOSE)
                                previousChainResult.right()
                            }
                        }
                        is Result.Failure -> {
                            delay(DELAY_SDK_DIALOG_CLOSE)
                            SaltPayExceptionHandler.handle(result.error)
                            ScanChainException.CheckForSaltPayOnboardingCaseException(result.error).left()
                        }
                    }
                }
            }
            cardTypesResolver.isSaltPayWallet() -> {
                delay(DELAY_SDK_DIALOG_CLOSE)
                if (previousChainResult.card.backupStatus?.isActive == false) {
                    SaltPayExceptionHandler.handle(SaltPayActivationError.PutVisaCard)
                    ScanChainException.PutSaltPayVisaCard.left()
                } else {
                    Analytics.setContext(previousChainResult)
                    previousChainResult.right()
                }
            }
            OnboardingHelper.isOnboardingCase(previousChainResult) -> {
                Analytics.addContext(previousChainResult)
                store.dispatchOnMain(GlobalAction.Onboarding.Start(previousChainResult, canSkipBackup = true))
                val appScreen = OnboardingHelper.whereToNavigate(previousChainResult)
                ScanChainException.OnboardingNeeded(appScreen).left()
            }
            else -> {
                Analytics.setContext(previousChainResult)
                // If twins was twinned previously but twins welcome not shown
                if (previousChainResult.twinsIsTwinned() && !preferencesStore.wasTwinsOnboardingShown()) {
                    store.dispatchOnMain(
                        TwinCardsAction.SetStepOfScreen(TwinCardsStep.WelcomeOnly(previousChainResult)),
                    )
                    ScanChainException.OnboardingNeeded(AppScreen.OnboardingTwins).left()
                } else {
                    delay(DELAY_SDK_DIALOG_CLOSE)
                    previousChainResult.right()
                }
            }
        }
    }
}