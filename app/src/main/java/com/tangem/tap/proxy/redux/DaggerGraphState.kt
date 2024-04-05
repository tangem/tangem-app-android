package com.tangem.tap.proxy.redux

import com.tangem.TangemSdkLogger
import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.feedback.FeedbackManagerFeatureToggles
import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.UserWalletsListManagerFeatureToggles
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.feature.qrscanning.QrScanningRouter
import com.tangem.features.managetokens.featuretoggles.ManageTokensFeatureToggles
import com.tangem.features.managetokens.navigation.ManageTokensUi
import com.tangem.features.send.api.featuretoggles.SendFeatureToggles
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.tester.api.TesterRouter
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.features.wallet.navigation.WalletRouter
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.tap.features.customtoken.api.featuretoggles.CustomTokenFeatureToggles
import com.tangem.tap.proxy.AppStateHolder
import org.rekotlin.StateType

data class DaggerGraphState(
    val testerRouter: TesterRouter? = null,
    val networkConnectionManager: NetworkConnectionManager? = null,
    val customTokenFeatureToggles: CustomTokenFeatureToggles? = null,
    val scanCardUseCase: ScanCardUseCase? = null,
    val walletRouter: WalletRouter? = null,
    val walletConnectRepository: WalletConnectRepository? = null,
    val walletConnectSessionsRepository: WalletConnectSessionsRepository? = null,
    val walletConnectInteractor: WalletConnectInteractor? = null,
    val tokenDetailsRouter: TokenDetailsRouter? = null,
    val manageTokensFeatureToggles: ManageTokensFeatureToggles? = null,
    val manageTokensUi: ManageTokensUi? = null,
    val scanCardProcessor: ScanCardProcessor? = null,
    val cardSdkConfigRepository: CardSdkConfigRepository? = null,
    val appCurrencyRepository: AppCurrencyRepository? = null,
    val walletManagersFacade: WalletManagersFacade? = null,
    val appStateHolder: AppStateHolder? = null,
    val appThemeModeRepository: AppThemeModeRepository? = null,
    val balanceHidingRepository: BalanceHidingRepository? = null,
    val walletsRepository: WalletsRepository? = null,
    val networksRepository: NetworksRepository? = null,
    val sendFeatureToggles: SendFeatureToggles? = null,
    val sendRouter: SendRouter? = null,
    val qrScanningRouter: QrScanningRouter? = null,
    val currenciesRepository: CurrenciesRepository? = null,
    val accountCreator: AccountCreator? = null,
    val userWalletsListManagerFeatureToggles: UserWalletsListManagerFeatureToggles? = null,
    val generalUserWalletsListManager: UserWalletsListManager? = null,
    val wasTwinsOnboardingShownUseCase: WasTwinsOnboardingShownUseCase? = null,
    val saveTwinsOnboardingShownUseCase: SaveTwinsOnboardingShownUseCase? = null,
    val cardRepository: CardRepository? = null,
    val feedbackManagerFeatureToggles: FeedbackManagerFeatureToggles? = null,
    val tangemSdkLogger: TangemSdkLogger? = null,
    val blockchainSDKLogger: BlockchainSDKLogger? = null,
    val settingsRepository: SettingsRepository? = null,
    val blockchainSDKFactory: BlockchainSDKFactory? = null,
) : StateType
