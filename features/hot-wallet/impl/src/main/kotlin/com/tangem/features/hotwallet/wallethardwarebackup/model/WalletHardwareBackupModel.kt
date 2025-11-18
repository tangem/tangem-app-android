package com.tangem.features.hotwallet.wallethardwarebackup.model

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.components.bottomsheets.message.icon
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.primaryButton
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.WalletHardwareBackupComponent
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.wallethardwarebackup.entity.WalletHardwareBackupUM
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class WalletHardwareBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase,
    private val urlOpener: UrlOpener,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val messageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<WalletHardwareBackupComponent.Params>()

    private val makeBackupAtFirstAlertBS
        get() = bottomSheetMessage {
            infoBlock {
                icon(R.drawable.ic_passcode_lock_32) {
                    type = MessageBottomSheetUMV2.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.hw_backup_need_finish_first)
                body = resourceReference(R.string.hw_backup_to_upgrade_description)
            }
            primaryButton {
                text = resourceReference(R.string.hw_backup_need_action)
                onClick {
                    router.push(
                        AppRoute.CreateWalletBackup(
                            userWalletId = params.userWalletId,
                            isUpgradeFlow = true,
                        ),
                    )
                    closeBs()
                }
            }
        }

    internal val uiState: StateFlow<WalletHardwareBackupUM>
        field = MutableStateFlow(
            WalletHardwareBackupUM(
                onBackClick = { router.pop() },
                blocks = persistentListOf(
                    WalletHardwareBackupUM.Block(
                        title = resourceReference(R.string.hw_backup_hardware_create_title),
                        titleLabel = LabelUM(
                            text = resourceReference(R.string.common_recommended),
                            style = LabelStyle.ACCENT,
                        ),
                        description = resourceReference(R.string.hw_backup_hardware_create_description),
                        onClick = ::onCreateNewWalletClick,
                    ),
                    WalletHardwareBackupUM.Block(
                        title = resourceReference(R.string.hw_backup_hardware_upgrade_title),
                        titleLabel = null,
                        description = resourceReference(R.string.hw_backup_hardware_upgrade_description),
                        onClick = ::onUpgradeCurrentWalletClick,
                    ),
                ),
                onBuyClick = ::onBuyClick,
            ),
        )

    init {
        showPurchaseBlockWithDelay()
    }

    private fun showPurchaseBlockWithDelay() {
        modelScope.launch {
            delay(SHOW_PURCHASE_BLOCK_DELAY)
            uiState.update { it.copy(showPurchaseBlock = true) }
        }
    }

    private fun onCreateNewWalletClick() {
        router.push(AppRoute.CreateHardwareWallet)
    }

    private fun onUpgradeCurrentWalletClick() {
        val userWallet = getUserWalletUseCase.invoke(params.userWalletId)
            .getOrElse { error("Cannot find user wallet with id: ${params.userWalletId.stringValue}") }
        if (userWallet is UserWallet.Hot) {
            if (!userWallet.backedUp) {
                messageSender.send(makeBackupAtFirstAlertBS)
            } else {
                val hotWalletId = userWallet.hotWalletId
                when (hotWalletId.authType) {
                    HotWalletId.AuthType.NoPassword -> {
                        router.push(AppRoute.UpgradeWallet(userWalletId = params.userWalletId))
                    }
                    HotWalletId.AuthType.Password,
                    HotWalletId.AuthType.Biometry,
                    -> modelScope.launch {
                        unlockHotWalletContextualUseCase.invoke(hotWalletId)
                            .onLeft {
                                Timber.e(it, "Unable to unlock wallet with id ${params.userWalletId}")
                            }
                            .onRight {
                                router.push(AppRoute.UpgradeWallet(userWalletId = params.userWalletId))
                            }
                    }
                }
            }
        }
    }

    private fun onBuyClick() {
        modelScope.launch {
            generateBuyTangemCardLinkUseCase.invoke().let { urlOpener.openUrl(it) }
        }
    }

    companion object {
        private const val SHOW_PURCHASE_BLOCK_DELAY = 3000L
    }
}