package com.tangem.data.visa

import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.visa.repository.VisaRepository
import com.tangem.domain.wallets.models.UserWalletId

internal class DummyVisaRepository : VisaRepository {

    override suspend fun getVisaCurrency(userWalletId: UserWalletId, isRefresh: Boolean): VisaCurrency {
        TODO(reason = "Implement in [REDACTED_JIRA]")
    }
}