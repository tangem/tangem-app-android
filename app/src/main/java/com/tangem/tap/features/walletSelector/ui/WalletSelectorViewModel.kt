package com.tangem.tap.features.walletSelector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.common.core.TangemError
import com.tangem.core.analytics.Analytics
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.common.analytics.events.MyWallets
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.domain.userWalletList.UserWalletsListError
import com.tangem.tap.domain.userWalletList.isLocked
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.walletSelector.redux.WalletSelectorAction
import com.tangem.tap.features.walletSelector.redux.WalletSelectorState
import com.tangem.tap.features.walletSelector.ui.model.DialogModel
import com.tangem.tap.features.walletSelector.ui.model.MultiCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.SingleCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.WarningModel
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletStoresManager
import kotlinx.coroutines.flow.*
import org.rekotlin.StoreSubscriber

internal class WalletSelectorViewModel : ViewModel(), StoreSubscriber<WalletSelectorState> {
    private val stateInternal = MutableStateFlow(WalletSelectorScreenState())
    val state: StateFlow<WalletSelectorScreenState> = stateInternal

    init {
        subscribeToStoreChanges()
        bootstrapWalletListChanges()
        bootstrapWalletsStoresChanges()
        bootstrapAppFiatCurrency()
    }

    fun unlock() {
        store.dispatch(WalletSelectorAction.UnlockWithBiometry)
    }

    fun addWallet() {
        store.dispatch(WalletSelectorAction.AddWallet)
    }

    fun walletClicked(userWalletId: UserWalletId) = with(state.value) {
        when {
            editingUserWalletsIds.isNotEmpty() && !editingUserWalletsIds.contains(userWalletId) -> {
                editWallet(userWalletId)
            }
            editingUserWalletsIds.isNotEmpty() && editingUserWalletsIds.contains(userWalletId) -> {
                cancelWalletEditing(userWalletId)
            }
            selectedUserWalletId != userWalletId -> {
                store.dispatch(
                    WalletSelectorAction.SelectWallet(
                        userWalletId = userWalletId,
                        sendAnalyticsEvent = true,
                    ),
                )
            }
        }
    }

    fun walletLongClicked(userWalletId: UserWalletId) = with(state.value) {
        if (editingUserWalletsIds.isEmpty()) {
            editWallet(userWalletId)
        }
    }

    fun cancelWalletsEditing() {
        stateInternal.update { prevState ->
            prevState.copy(
                editingUserWalletsIds = emptyList(),
            )
        }
    }

    fun renameWallet() = with(state.value) {
        if (editingUserWalletsIds.isNotEmpty() && dialog == null) {
            val editedUserWalletId = editingUserWalletsIds.first()
            val editedUserWallet = (multiCurrencyWallets + singleCurrencyWallets)
                .find { it.id == editedUserWalletId }

            if (editedUserWallet != null) {
                Analytics.send(MyWallets.Button.EditWalletTapped())
                val dialog = DialogModel.RenameWalletDialog(
                    currentName = editedUserWallet.name,
                    onConfirm = { newName ->
                        store.dispatch(WalletSelectorAction.RenameWallet(editedUserWalletId, newName))
                        stateInternal.update { prevState ->
                            prevState.copy(
                                dialog = null,
                                editingUserWalletsIds = emptyList(),
                            )
                        }
                    },
                    onDismiss = {
                        stateInternal.update { prevState ->
                            prevState.copy(
                                dialog = null,
                            )
                        }
                    },
                )

                stateInternal.update { prevState ->
                    prevState.copy(
                        dialog = dialog,
                    )
                }
            }
        }
    }

    fun deleteWallets() = with(state.value) {
        if (editingUserWalletsIds.isNotEmpty()) {
            val dialog = DialogModel.RemoveWalletDialog(
                onConfirm = {
                    store.dispatch(WalletSelectorAction.RemoveWallets(editingUserWalletsIds))
                    stateInternal.update { prevState ->
                        prevState.copy(
                            dialog = null,
                            editingUserWalletsIds = emptyList(),
                        )
                    }
                },
                onDismiss = {
                    stateInternal.update { prevState ->
                        prevState.copy(
                            dialog = null,
                        )
                    }
                },
            )

            stateInternal.update { prevState ->
                prevState.copy(
                    dialog = dialog,
                )
            }
        }
    }

    fun closeError() {
        store.dispatch(WalletSelectorAction.CloseError)
    }

    // TODO: Refactor errors handling
    override fun newState(state: WalletSelectorState) {
        stateInternal.update { prevState ->
            val walletsUi = state.wallets.toUiModels(state.fiatCurrency)
            val walletsIds = walletsUi.map { it.id }
            val multiCurrencyWallets = arrayListOf<MultiCurrencyUserWalletItem>()
            val singleCurrencyWallets = arrayListOf<SingleCurrencyUserWalletItem>()
            walletsUi.forEach { wallet ->
                when (wallet) {
                    is MultiCurrencyUserWalletItem -> {
                        multiCurrencyWallets.add(wallet)
                    }
                    is SingleCurrencyUserWalletItem -> {
                        singleCurrencyWallets.add(wallet)
                    }
                }
            }
            val warningDialog = createWarningDialogIfNeeded(state.error, prevState.dialog)

            prevState.copy(
                multiCurrencyWallets = multiCurrencyWallets,
                singleCurrencyWallets = singleCurrencyWallets,
                selectedUserWalletId = state.selectedWalletId,
                editingUserWalletsIds = prevState.editingUserWalletsIds.filter { it in walletsIds },
                isLocked = state.isLocked,
                showUnlockProgress = state.isUnlockInProgress,
                showAddCardProgress = state.isCardSavingInProgress,
                dialog = warningDialog,
                error = state.error
                    ?.takeIf { !it.silent && warningDialog == null }
                    ?.let { error ->
                        error.messageResId?.let { TextReference.Res(it) }
                            ?: TextReference.Str(error.customMessage)
                    },
            )
        }
    }

    override fun onCleared() {
        store.unsubscribe(this)
    }

    private fun createWarningDialogIfNeeded(error: TangemError?, currentDialog: DialogModel?): DialogModel? {
        return when (error) {
            is UserWalletsListError.BiometricsAuthenticationLockout -> WarningModel.BiometricsLockoutWarning(
                isPermanent = error.isPermanent,
                onDismiss = this::dismissWarningDialog,
            )
            is UserWalletsListError.EncryptionKeyInvalidated -> WarningModel.KeyInvalidatedWarning(
                onDismiss = this::dismissWarningDialog,
            )
            is UserWalletsListError.BiometricsAuthenticationDisabled -> WarningModel.BiometricsDisabledWarning(
                onDismiss = this::clearUserWallets,
            )
            else -> currentDialog
        }
    }

    private fun clearUserWallets() {
        store.dispatch(WalletSelectorAction.ClearUserWallets)
    }

    private fun dismissWarningDialog() {
        stateInternal.update { prevState ->
            prevState.copy(
                dialog = null,
            )
        }
        closeError()
    }

    private fun editWallet(userWalletId: UserWalletId) {
        stateInternal.update { prevState ->
            prevState.copy(
                editingUserWalletsIds = prevState.editingUserWalletsIds + userWalletId,
            )
        }
    }

    private fun cancelWalletEditing(userWalletId: UserWalletId) {
        stateInternal.update { prevState ->
            prevState.copy(
                editingUserWalletsIds = prevState.editingUserWalletsIds - userWalletId,
            )
        }
    }

    private fun subscribeToStoreChanges() {
        store.subscribe(this) { appState ->
            appState.skip { old, new -> old.walletSelectorState == new.walletSelectorState }
                .select { it.walletSelectorState }
        }
    }

    private fun bootstrapWalletListChanges() {
        userWalletsListManager.userWallets
            .onEach {
                store.dispatchOnMain(WalletSelectorAction.UserWalletsLoaded(userWallets = it))
            }
            .launchIn(viewModelScope)

        userWalletsListManager.selectedUserWallet
            .onEach {
                store.dispatchOnMain(WalletSelectorAction.SelectedWalletChanged(selectedWallet = it))
            }
            .launchIn(viewModelScope)

        userWalletsListManager.isLocked
            .onEach {
                store.dispatchOnMain(WalletSelectorAction.IsLockedChanged(isLocked = it))
            }
            .launchIn(viewModelScope)
    }

    private fun bootstrapWalletsStoresChanges() {
        walletStoresManager.getAll()
            .onEach { walletStores ->
                store.dispatchOnMain(WalletSelectorAction.WalletStoresChanged(walletStores))
            }
            .launchIn(viewModelScope)
    }

    private fun bootstrapAppFiatCurrency() {
        store.dispatch(
            WalletSelectorAction.ChangeAppCurrency(
                fiatCurrency = store.state.globalState.appCurrency,
            ),
        )
    }
}