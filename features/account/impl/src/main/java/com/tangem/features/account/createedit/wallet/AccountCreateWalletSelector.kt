package com.tangem.features.account.createedit.wallet

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.createedit.entity.AccountCreateEditUM
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Resolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** The wallet the new account is created in and the row showing it, switched through [picker]. */
internal class AccountCreateWalletSelector(
    initialWalletId: UserWalletId,
    private val scope: CoroutineScope,
    val picker: AccountWalletPicker,
    onFullWalletPicked: () -> Unit,
) {

    val selectedWalletId: StateFlow<UserWalletId>
        field = MutableStateFlow(initialWalletId)

    val walletRow: Flow<AccountCreateEditUM.Wallet?> = combine(
        picker.wallets.filterNotNull(),
        selectedWalletId,
    ) { wallets, selectedId ->
        val selected = wallets.firstOrNull { it.walletId == selectedId }
        if (wallets.size > 1 && selected != null) {
            AccountCreateEditUM.Wallet(name = selected.name, onClick = ::openChooser)
        } else {
            null
        }
    }

    private var openChooserJob: Job? = null

    init {
        picker.picks
            .onEach { pick ->
                when (pick) {
                    is AccountWalletPicker.Pick.Wallet -> select(pick.walletId)
                    AccountWalletPicker.Pick.Full -> onFullWalletPicked()
                }
            }
            .launchIn(scope)
    }

    fun select(walletId: UserWalletId) {
        selectedWalletId.value = walletId
    }

    private fun openChooser() {
        if (openChooserJob?.isActive == true) return
        openChooserJob = scope.launch {
            val resolution = picker.resolve(hasRoom = AccountList::canAddMoreCryptoAccounts)
            picker.openChooser(fullWalletIds = (resolution as? Resolution.Choose)?.fullWalletIds.orEmpty())
        }
    }
}