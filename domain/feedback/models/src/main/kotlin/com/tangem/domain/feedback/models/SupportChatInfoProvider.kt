package com.tangem.domain.feedback.models

import com.tangem.domain.models.wallet.UserWalletId

/**
 * @property topic the chat topic whose attached logs carry this block
 */
interface SupportChatInfoProvider {

    val topic: SupportChatTopic

    /** @return the block as `key: value` lines, read at the moment the logs are attached */
    suspend fun getInfo(userWalletId: UserWalletId): String
}