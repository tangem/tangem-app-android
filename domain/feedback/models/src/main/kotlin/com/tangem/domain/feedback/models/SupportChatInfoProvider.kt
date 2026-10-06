package com.tangem.domain.feedback.models

import com.tangem.domain.models.wallet.UserWalletId

interface SupportChatInfoProvider {

    /** @return the block as `key: value` lines, read at the moment the logs are attached */
    suspend fun getInfo(userWalletId: UserWalletId): String
}