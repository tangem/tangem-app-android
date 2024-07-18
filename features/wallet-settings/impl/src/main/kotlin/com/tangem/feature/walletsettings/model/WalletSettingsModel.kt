package com.tangem.feature.walletsettings.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
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
import javax.inject.Inject

@Suppress("LongParameterList")
@ComponentScoped
internal class WalletSettingsModel @Inject constructor(
    getWalletUseCase: GetUserWalletUseCase,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val itemsBuilder: ItemsBuilder,
    override val dispatchers: CoroutineDispatcherProvider,
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
                val wallet = maybeWallet.getOrElse { error ->
                    error(
                        """
                        Failed to get user wallet
                        |- User wallet ID: $params
                        |- Cause: $error
                        """.trimIndent(),
                    )
                }

                state.update { value ->
                    value.copy(items = buildItems(wallet, dialogNavigation))
                }
            }
            .launchIn(modelScope)
    }

    private fun buildItems(
        userWallet: UserWallet,
        dialogNavigation: SlotNavigation<DialogConfig>,
    ): PersistentList<WalletSettingsItemUM> = itemsBuilder.buildItems(
        userWalletId = userWallet.walletId,
        userWalletName = userWallet.name,
        isReferralAvailable = userWallet.cardTypesResolver.isTangemWallet(),
        renameWallet = { openRenameWalletDialog(userWallet, dialogNavigation) },
        forgetWallet = { forgetWallet(userWallet.walletId) },
    )

    private fun openRenameWalletDialog(userWallet: UserWallet, dialogNavigation: SlotNavigation<DialogConfig>) {
        val config = DialogConfig.RenameWallet(
            userWalletId = userWallet.walletId,
            currentName = userWallet.name,
        )

        dialogNavigation.activate(config)
    }

    private fun forgetWallet(userWalletId: UserWalletId) = modelScope.launch {
        val hasUserWallets = deleteWalletUseCase(userWalletId).getOrElse {
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
}