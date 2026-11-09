package com.tangem.tap.domain.userWalletList.utils

import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.tap.domain.userWalletList.model.HotWalletIdDTO
import com.tangem.utils.converter.TwoWayConverter

internal object HotWalletIdConverter : TwoWayConverter<HotWalletId, HotWalletIdDTO> {

    override fun convert(value: HotWalletId): HotWalletIdDTO {
        return HotWalletIdDTO(value = value.value, authType = value.authType)
    }

    override fun convertBack(value: HotWalletIdDTO): HotWalletId {
        return HotWalletId(value = value.value, authType = value.authType)
    }
}