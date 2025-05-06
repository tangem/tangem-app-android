package com.tangem.data.wallets.converters

import com.tangem.datasource.api.tangemTech.models.WalletResponse
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.models.UserWalletRemoteInfo
import com.tangem.utils.converter.Converter

internal object UserWalletRemoteInfoConverter : Converter<WalletResponse, UserWalletRemoteInfo> {
    override fun convert(value: WalletResponse): UserWalletRemoteInfo {
        return UserWalletRemoteInfo(
            walletId = UserWalletId(value.id),
            name = value.name.orEmpty(),
            isNotificationsEnabled = value.notifyStatus,
        )
    }
}