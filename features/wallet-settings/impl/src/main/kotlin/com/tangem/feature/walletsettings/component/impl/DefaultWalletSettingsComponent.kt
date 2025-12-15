package com.tangem.feature.walletsettings.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableDialogComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.utils.requestPermission
import com.tangem.datasource.local.accounts.AccountTokenMigrationStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.feature.walletsettings.component.NetworksAvailableForNotificationsComponent
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.DialogConfig
import com.tangem.feature.walletsettings.entity.NetworksAvailableForNotificationBSConfig
import com.tangem.feature.walletsettings.impl.R
import com.tangem.feature.walletsettings.model.WalletSettingsModel
import com.tangem.feature.walletsettings.ui.WalletSettingsScreen
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class DefaultWalletSettingsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: WalletSettingsComponent.Params,
    private val renameWalletComponentFactory: RenameWalletComponent.Factory,
    private val networksAvailableForNotificationsComponent: NetworksAvailableForNotificationsComponent.Factory,
    private val accountTokenMigrationStore: AccountTokenMigrationStore,
    private val accountsFeatureToggles: AccountsFeatureToggles,
) : WalletSettingsComponent, AppComponentContext by context {

    private val model: WalletSettingsModel = getOrCreateModel(params)

    private val accountMigrationJobHolder = JobHolder()

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        key = "bottomSheetSlot",
        childFactory = ::bottomSheetChild,
    )

    private val dialog = childSlot(
        source = model.dialogNavigation,
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
        key = "dialogSlot",
        childFactory = ::dialogChild,
    )

    init {
        lifecycle.subscribe(
            onResume = {
                if (accountsFeatureToggles.isFeatureEnabled) {
                    showMigrationAlertIfNeeded()
                }
            },
            onPause = { accountMigrationJobHolder.cancel() },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val dialog by dialog.subscribeAsState()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        WalletSettingsScreen(
            modifier = modifier,
            state = state,
            dialog = { dialog.child?.instance?.Dialog() },
        )

        val requestPushPermission = requestPermission(
            onAllow = { state.onPushNotificationPermissionGranted(true) },
            onDeny = { state.onPushNotificationPermissionGranted(false) },
            permission = PUSH_PERMISSION,
        )

        if (state.hasRequestPushNotificationsPermission) {
            LaunchedEffect(Unit) {
                requestPushPermission()
            }
        }

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun dialogChild(
        dialogConfig: DialogConfig,
        componentContext: ComponentContext,
    ): ComposableDialogComponent = when (dialogConfig) {
        is DialogConfig.RenameWallet -> {
            renameWalletComponentFactory.create(
                context = childByContext(componentContext),
                params = RenameWalletComponent.Params(
                    userWalletId = dialogConfig.userWalletId,
                    currentName = dialogConfig.currentName,
                    onDismiss = model.dialogNavigation::dismiss,
                ),
            )
        }
    }

    @Suppress("UnusedPrivateMember")
    private fun bottomSheetChild(
        config: NetworksAvailableForNotificationBSConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = networksAvailableForNotificationsComponent.create(
        context = childByContext(componentContext),
        params = NetworksAvailableForNotificationsComponent.Params(
            onDismiss = model.bottomSheetNavigation::dismiss,
        ),
    )

    private fun showMigrationAlertIfNeeded() {
        accountTokenMigrationStore.get(params.userWalletId)
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { (fromAccount, toAccount) ->
                messageSender.send(
                    DialogMessage(
                        title = resourceReference(R.string.accounts_migration_alert_title),
                        message = resourceReference(
                            R.string.accounts_migration_alert_message,
                            wrappedList(fromAccount, toAccount),
                        ),
                        firstActionBuilder = {
                            EventMessageAction(
                                title = resourceReference(R.string.common_got_it),
                                onClick = { /* no-op */ },
                            )
                        },
                        onDismissRequest = {
                            componentScope.launch {
                                accountTokenMigrationStore.remove(params.userWalletId)
                            }
                        },
                    ),
                )
            }
            .launchIn(componentScope)
            .saveIn(accountMigrationJobHolder)
    }

    @AssistedFactory
    interface Factory : WalletSettingsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: WalletSettingsComponent.Params,
        ): DefaultWalletSettingsComponent
    }
}