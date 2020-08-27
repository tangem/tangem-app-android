package com.tangem.tap.features.wallet.redux

import android.graphics.Bitmap
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.PayIdData
import org.rekotlin.StateType

data class WalletState(
        val state: ProgressState = ProgressState.Done,
        val cardImage: Bitmap? = null,
        val walletManager: WalletManager? = null,
        val wallet: Wallet? = null,
        val currencyData: BalanceWidgetData = BalanceWidgetData(),
        val payIdData: PayIdData = PayIdData(),
        val qrCode: Bitmap? = null
) : StateType


enum class ProgressState { Loading, Done, Error }