package com.tangem.features.hotwallet.walletbackup.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.WalletBackupComponent
import com.tangem.features.hotwallet.walletbackup.entity.BackupStatus
import com.tangem.features.hotwallet.walletbackup.entity.WalletBackupUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class WalletBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val urlOpener: UrlOpener,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params: WalletBackupComponent.Params = paramsContainer.require()

    val uiState: StateFlow<WalletBackupUM>
        field = MutableStateFlow(
            WalletBackupUM(
                onBackClick = { router.pop() },
                hardwareWalletOption = LabelUM(
                    text = resourceReference(R.string.common_recommended),
                    style = LabelStyle.ACCENT,
                ),
                recoveryPhraseOption = LabelUM(
                    text = resourceReference(R.string.hw_backup_no_backup),
                    style = LabelStyle.WARNING,
                ),
                googleDriveOption = LabelUM(
                    text = resourceReference(R.string.common_coming_soon),
                    style = LabelStyle.REGULAR,
                ),
                googleDriveStatus = BackupStatus.ComingSoon,
                onBuyClick = ::onBuyClick,
                onRecoveryPhraseClick = ::onRecoveryPhraseClick,
                onGoogleDriveClick = { },
                onHardwareWalletClick = ::onHardwareWalletClick,
                backedUp = false,
            ),
        )

    init {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.BackupScreenOpened(isManualBackupEnabled = true))
        getUserWalletUseCase.invokeFlow(params.userWalletId)
            .onEach { either ->
                either.fold(
                    ifLeft = {
                        Timber.e("Error on getting user wallet: $it")
                    },
                    ifRight = {
                        updateBackupStatuses(it)
                    },
                )
            }.launchIn(modelScope)
    }

    private fun updateBackupStatuses(userWallet: UserWallet) {
        uiState.update { currentState ->
            if (userWallet is UserWallet.Hot) {
                currentState.updateBackupStatusesHotWallet(userWallet)
            } else {
                currentState
            }
        }
    }

    private fun WalletBackupUM.updateBackupStatusesHotWallet(userWallet: UserWallet.Hot): WalletBackupUM = copy(
        recoveryPhraseOption = if (userWallet.backedUp) {
            LabelUM(
                text = resourceReference(R.string.common_done),
                style = LabelStyle.ACCENT,
            )
        } else {
            LabelUM(
                text = resourceReference(R.string.hw_backup_no_backup),
                style = LabelStyle.WARNING,
            )
        },
        googleDriveOption = LabelUM(
            text = resourceReference(R.string.common_coming_soon),
            style = LabelStyle.REGULAR,
        ),
        backedUp = userWallet.backedUp,
    )

    private fun onBuyClick() {
        analyticsEventHandler.send(Basic.ButtonBuy(source = AnalyticsParam.ScreensSources.Backup))
        modelScope.launch {
            generateBuyTangemCardLinkUseCase.invoke().let { urlOpener.openUrl(it) }
        }
    }

    private fun onRecoveryPhraseClick() {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonRecoveryPhrase)
        if (uiState.value.backedUp) {
            getUserWalletUseCase.invoke(params.userWalletId)
                .fold(
                    ifLeft = {
                        Timber.e("Error on getting user wallet: $it")
                    },
                    ifRight = { userWallet ->
                        when (userWallet) {
                            is UserWallet.Cold -> {
                                val userWalletId = userWallet.walletId
                                Timber.e("Unexpected cold wallet when request seed phrase: $userWalletId")
                            }
                            is UserWallet.Hot -> showSeedPhrase(userWallet)
                        }
                    },
                )
        } else {
            router.push(
                AppRoute.CreateWalletBackup(
                    userWalletId = params.userWalletId,
                    analyticsSource = AnalyticsParam.ScreensSources.Backup.value,
                    analyticsAction = WalletSettingsAnalyticEvents.RecoveryPhraseScreenAction.Backup.value,
                ),
            )
        }
    }

    private fun showSeedPhrase(hotWallet: UserWallet.Hot) {
        modelScope.launch {
            unlockHotWalletContextualUseCase.invoke(hotWallet.hotWalletId)
                .fold(
                    ifLeft = {
                        Timber.e("Error while export seed phrase: $it")
                    },
                    ifRight = { seedPhrasePrivateInfo ->
                        router.push(AppRoute.ViewPhrase(params.userWalletId))
                    },
                )
        }
    }

    private fun onHardwareWalletClick() {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonHardwareUpdate)
        router.push(AppRoute.WalletHardwareBackup(params.userWalletId))
    }
}