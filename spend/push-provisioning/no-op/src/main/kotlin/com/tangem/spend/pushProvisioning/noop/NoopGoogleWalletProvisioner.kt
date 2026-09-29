package com.tangem.spend.pushProvisioning.noop

import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Card
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.CardStatus
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Environment
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.FailureReason
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class NoopGoogleWalletProvisioner @Inject constructor() : GoogleWalletProvisioner {

    override val environment: Environment? = null

    override val isAdditionInProgress: Boolean = false

    override val walletChanges: Flow<Unit> = flow { awaitCancellation() }

    override suspend fun getCardStatus(lastFourDigits: String): CardStatus = CardStatus.Unsupported

    override suspend fun addCard(card: Card): AddResult = NOT_INCLUDED

    override suspend fun resumeAddingCard(lastFourDigits: String, displayName: String): AddResult = NOT_INCLUDED

    private companion object {
        val NOT_INCLUDED = AddResult.Failed(
            reason = FailureReason.UNSUPPORTED,
            description = "Push provisioning is not included in this build",
        )
    }
}