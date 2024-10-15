package com.tangem.tap.proxy.redux

import com.tangem.TangemSdkLogger
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.signer.TransactionSignerFactory
import com.tangem.common.routing.AppRouter
import com.tangem.core.navigation.email.EmailSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.feature.qrscanning.QrScanningRouter
import com.tangem.features.pushnotifications.api.navigation.PushNotificationsRouter
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.staking.api.navigation.StakingRouter
import com.tangem.features.tester.api.TesterRouter
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.features.wallet.navigation.WalletRouter
import com.tangem.tap.domain.scanCard.CardScanningFeatureToggles
import com.tangem.tap.domain.walletconnect2.domain.LegacyWalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.tap.features.home.featuretoggles.HomeFeatureToggles
import com.tangem.tap.proxy.AppStateHolder
import org.rekotlin.StateType

data class DaggerGraphState(
    val testerRouter: TesterRouter? = null,
    val networkConnectionManager: NetworkConnectionManager? = null,
    val cardScanningFeatureToggles: CardScanningFeatureToggles? = null,
    val scanCardUseCase: ScanCardUseCase? = null,
    val walletRouter: WalletRouter? = null,
    val walletConnectRepository: LegacyWalletConnectRepository? = null,
    val walletConnectSessionsRepository: WalletConnectSessionsRepository? = null,
    val walletConnectInteractor: WalletConnectInteractor? = null,
    val tokenDetailsRouter: TokenDetailsRouter? = null,
    val scanCardProcessor: ScanCardProcessor? = null,
    val cardSdkConfigRepository: CardSdkConfigRepository? = null,
    val appCurrencyRepository: AppCurrencyRepository? = null,
    val walletManagersFacade: WalletManagersFacade? = null,
    val appStateHolder: AppStateHolder? = null,
    val appThemeModeRepository: AppThemeModeRepository? = null,
    val balanceHidingRepository: BalanceHidingRepository? = null,
    val walletsRepository: WalletsRepository? = null,
    val networksRepository: NetworksRepository? = null,
    val sendRouter: SendRouter? = null,
    val qrScanningRouter: QrScanningRouter? = null,
    val currenciesRepository: CurrenciesRepository? = null,
    val generalUserWalletsListManager: UserWalletsListManager? = null,
    val wasTwinsOnboardingShownUseCase: WasTwinsOnboardingShownUseCase? = null,
    val saveTwinsOnboardingShownUseCase: SaveTwinsOnboardingShownUseCase? = null,
    val generateWalletNameUseCase: GenerateWalletNameUseCase? = null,
    val cardRepository: CardRepository? = null,
    val tangemSdkLogger: TangemSdkLogger? = null,
    val settingsRepository: SettingsRepository? = null,
    val blockchainSDKFactory: BlockchainSDKFactory? = null,
    val emailSender: EmailSender? = null,
    val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase? = null,
    val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase? = null,
    val getCardInfoUseCase: GetCardInfoUseCase? = null,
    val assetLoader: AssetLoader? = null,
    val stakingRouter: StakingRouter? = null,
    val urlOpener: UrlOpener? = null,
    val shareManager: ShareManager? = null,
    val appRouter: AppRouter? = null,
    val pushNotificationsRouter: PushNotificationsRouter? = null,
    val transactionSignerFactory: TransactionSignerFactory? = null,
    val homeFeatureToggles: HomeFeatureToggles? = null,
    val getUserCountryUseCase: GetUserCountryUseCase? = null,
) : StateType
