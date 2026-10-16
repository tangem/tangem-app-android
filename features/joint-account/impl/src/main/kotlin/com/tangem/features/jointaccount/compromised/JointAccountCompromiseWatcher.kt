package com.tangem.features.jointaccount.compromised

import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.verifier.JointAccountCompositionVerifier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transform
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Lets a screen show only the joint account data that verifies: every backend update is checked before it reaches
 * the screen, and the first one that does not closes the screen with the alert instead.
 */
internal class JointAccountCompromiseWatcher @Inject constructor(
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val compositionVerifier: JointAccountCompositionVerifier,
    private val jointAccountCompromisedAlert: JointAccountCompromisedAlert,
) {

    fun verified(
        userWalletId: UserWalletId,
        accounts: Flow<JointAccount?>,
        onCompromised: () -> Unit,
    ): Flow<JointAccount?> {
        val isClosed = AtomicBoolean(false)

        return accounts.transform { account ->
            when {
                isClosed.get() -> Unit
                account == null || isVerified(userWalletId = userWalletId, jointAccount = account) -> emit(account)
                isClosed.compareAndSet(false, true) -> {
                    onCompromised()
                    jointAccountCompromisedAlert.show(userWalletId = userWalletId)
                }
            }
        }
    }

    private suspend fun isVerified(userWalletId: UserWalletId, jointAccount: JointAccount): Boolean {
        val accountId = AccountId.forJointAccount(userWalletId = userWalletId, value = jointAccount.cryptoAccountId)
            .getOrNull()
            ?: return false

        // Right after creating or joining, the account may not have reached the wallet's account list yet: the update
        // waits for it, as there is nothing to verify against without its owner key index
        val account = singleAccountListSupplier(userWalletId)
            .mapNotNull { accountList ->
                accountList.accounts.firstOrNull { it.accountId == accountId } as? Account.Joint
            }
            .firstOrNull()
            ?: return false

        return compositionVerifier.isVerified(
            userWalletId = userWalletId,
            ownerKeyIndex = account.ownerKeyIndex,
            jointAccount = jointAccount,
        )
    }
}