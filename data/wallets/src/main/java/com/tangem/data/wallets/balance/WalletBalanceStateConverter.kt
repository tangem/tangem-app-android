package com.tangem.data.wallets.balance

import com.tangem.domain.wallets.models.WalletBalanceState
import com.tangem.utils.converter.TwoWayConverter

internal object WalletBalanceStateConverter : TwoWayConverter<WalletBalanceStateDTO, WalletBalanceState> {

    override fun convert(value: WalletBalanceStateDTO): WalletBalanceState = when (value) {
        WalletBalanceStateDTO.ToppedUp -> WalletBalanceState.ToppedUp
        WalletBalanceStateDTO.Empty -> WalletBalanceState.Empty
        WalletBalanceStateDTO.Error -> WalletBalanceState.Error
    }

    override fun convertBack(value: WalletBalanceState): WalletBalanceStateDTO = when (value) {
        WalletBalanceState.ToppedUp -> WalletBalanceStateDTO.ToppedUp
        WalletBalanceState.Empty -> WalletBalanceStateDTO.Empty
        WalletBalanceState.Error -> WalletBalanceStateDTO.Error
    }
}