package com.tangem.feature.walletsettings.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.utils.AnalyticsContextProxy
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsSyncUseCase
import com.tangem.feature.walletsettings.analytics.Settings
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.DialogConfig
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.entity.WalletSettingsUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.feature.walletsettings.utils.ItemsBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class WalletSettingsModel @Inject constructor(
    getWalletUseCase: GetUserWalletUseCase,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val itemsBuilder: ItemsBuilder,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val analyticsContextProxy: AnalyticsContextProxy,
    private val reduxStateHolder: ReduxStateHolder,
    private val getShouldSaveUserWalletsSyncUseCase: ShouldSaveUserWalletsSyncUseCase,
) : Model() {

    val params: WalletSettingsComponent.Params = paramsContainer.require()
    val dialogNavigation = SlotNavigation<DialogConfig>()

    val state: MutableStateFlow<WalletSettingsUM> = MutableStateFlow(
        value = WalletSettingsUM(
            popBack = router::pop,
            items = persistentListOf(),
        ),
    )

    init {
        getWalletUseCase.invokeFlow(params.userWalletId)
            .distinctUntilChanged()
            .onEach { maybeWallet ->
                val wallet = maybeWallet.getOrNull() ?: return@onEach
                val isRenameWalletAvailable = getShouldSaveUserWalletsSyncUseCase()
                state.update { value ->
                    value.copy(items = buildItems(wallet, dialogNavigation, isRenameWalletAvailable))
                }
            }
            .launchIn(modelScope)
    }

    private fun buildItems(
        userWallet: UserWallet,
        dialogNavigation: SlotNavigation<DialogConfig>,
        isRenameWalletAvailable: Boolean,
    ): PersistentList<WalletSettingsItemUM> = itemsBuilder.buildItems(
        userWalletId = userWallet.walletId,
        userWalletName = userWallet.name,
        isReferralAvailable = userWallet.cardTypesResolver.isTangemWallet(),
        isLinkMoreCardsAvailable = userWallet.scanResponse.card.backupStatus == CardDTO.BackupStatus.NoBackup,
        isManageTokensAvailable = userWallet.isMultiCurrency,
        isRenameWalletAvailable = isRenameWalletAvailable,
        renameWallet = { openRenameWalletDialog(userWallet, dialogNavigation) },
        forgetWallet = {
            val message = DialogMessage(
                message = resourceReference(R.string.user_wallet_list_delete_prompt),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_delete),
                        warning = true,
                        onClick = ::forgetWallet,
                    )
                },
                secondActionBuilder = { cancelAction() },
            )

            messageSender.send(message)
        },
        onLinkMoreCardsClick = {
            onLinkMoreCardsClick(scanResponse = userWallet.scanResponse)
        },
    )

    private fun openRenameWalletDialog(userWallet: UserWallet, dialogNavigation: SlotNavigation<DialogConfig>) {
        val config = DialogConfig.RenameWallet(
            userWalletId = userWallet.walletId,
            currentName = userWallet.name,
        )

        dialogNavigation.activate(config)
    }

    private fun forgetWallet() = modelScope.launch {
        val hasUserWallets = deleteWalletUseCase(params.userWalletId).getOrElse {
            Timber.e("Unable to delete wallet: $it")

            messageSender.send(
                message = SnackbarMessage(resourceReference(R.string.common_unknown_error)),
            )

            return@launch
        }

        if (hasUserWallets) {
            router.pop()
        } else {
            router.replaceAll(AppRoute.Home)
        }
    }

    private fun onLinkMoreCardsClick(scanResponse: ScanResponse) {
        analyticsEventHandler.send(Settings.ButtonCreateBackup)

        analyticsContextProxy.addContext(scanResponse)

        reduxStateHolder.dispatch(
            LegacyAction.StartOnboardingProcess(
                scanResponse = scanResponse,
                canSkipBackup = false,
            ),
        )

        router.push(AppRoute.OnboardingWallet())
    }
}