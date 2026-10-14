package com.tangem.features.jointaccount.compromised

import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import javax.inject.Inject

/** Closes a screen that shows a joint account's backend data once that account stops verifying */
internal class JointAccountCompromiseWatcher @Inject constructor(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val jointAccountCompromisedAlert: JointAccountCompromisedAlert,
) {

    fun watch(userWalletId: UserWalletId, account: Flow<JointAccount?>, scope: CoroutineScope, onClose: () -> Unit) {
        combine(account, singleAccountStatusListSupplier(userWalletId)) { jointAccount, statusList ->
            val cryptoAccountId = jointAccount?.cryptoAccountId ?: return@combine false
            val status = statusList.accountStatuses.firstOrNull { it.accountId.value == cryptoAccountId }

            (status as? AccountStatus.Joint)?.value is JointAccountStatusValue.Error.Compromised
        }
            .filter { isCompromised -> isCompromised }
            .take(count = 1)
            .onEach {
                onClose()
                jointAccountCompromisedAlert.show(userWalletId = userWalletId)
            }
            .launchIn(scope)
    }
}