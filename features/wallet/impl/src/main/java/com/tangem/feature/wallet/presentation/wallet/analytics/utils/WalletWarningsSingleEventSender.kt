package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.common.routing.AppRoute.WalletActivation
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.components.bottomsheets.message.icon
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.primaryButton
import com.tangem.core.ui.components.bottomsheets.message.secondaryButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SeedPhraseNotificationUseCase
import com.tangem.feature.wallet.child.wallet.model.WalletActivationBannerType
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@ModelScoped
internal class WalletWarningsSingleEventSender @Inject constructor(
    private val seedPhraseNotificationUseCase: SeedPhraseNotificationUseCase,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
    private val uiMessageSender: UiMessageSender,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val router: Router,
) {
    private val isActivationBottomSheetShown: ConcurrentHashMap<UserWalletId, Boolean> = ConcurrentHashMap()

    suspend fun send(
        userWalletId: UserWalletId,
        displayedUiState: WalletState?,
        newWarnings: List<WalletNotification>,
    ) {
        if (screenLifecycleProvider.isBackgroundState.value) return
        if (newWarnings.isEmpty()) return
        if (displayedUiState == null || displayedUiState.pullToRefreshConfig.isRefreshing) return

        val events = newWarnings.filter { it !in displayedUiState.warnings }

        // We must show activation bs only for the first seen wallet when open the app (if need, see conditions below),
        // so we keep this wallet id and use for future checks, ignore other wallets during the app session.
        if (isActivationBottomSheetShown.isEmpty()) {
            isActivationBottomSheetShown[userWalletId] = false
        }

        events.forEach { event ->
            when (event) {
                is WalletNotification.Critical.SeedPhraseNotification -> {
                    seedPhraseNotificationUseCase.notified(userWalletId = userWalletId)
                }
                is WalletNotification.FinishWalletActivation -> {
                    // We check that map contains the first seen wallet (will return null instead false/true otherwise)
                    // and for this wallet we haven't shown the activation bs yet (check that returns false, not true)
                    if (isActivationBottomSheetShown[userWalletId] == false) {
                        if (event.type == WalletActivationBannerType.Warning) {
                            showFinishActivationBottomSheet(userWalletId)
                        }
                        isActivationBottomSheetShown[userWalletId] = true
                    }
                }
                else -> Unit
            }
        }
    }

    private fun showFinishActivationBottomSheet(userWalletId: UserWalletId) {
        val userWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: return
        if (userWallet !is UserWallet.Hot) return

        val message = bottomSheetMessage {
            infoBlock {
                icon(R.drawable.img_knight_shield_32) {
                    type = MessageBottomSheetUMV2.Icon.Type.Warning
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.hw_activation_need_title)
                body = resourceReference(R.string.hw_activation_need_description)
            }
            secondaryButton {
                text = resourceReference(R.string.common_later)
                onClick {
                    closeBs()
                }
            }
            primaryButton {
                text = resourceReference(R.string.hw_activation_need_backup)
                onClick {
                    router.push(WalletActivation(userWallet.walletId, userWallet.backedUp))
                    closeBs()
                }
            }
        }

        uiMessageSender.send(message)
    }
}