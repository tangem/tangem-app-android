package com.tangem.tap.features.demo

import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.common.ScanResponse

/**
 * Created by Anton Zhilenkov on 21/02/2022.
 */
fun ScanResponse.isDemoCard(): Boolean = DemoHelper.isDemoCardId(card.cardId)
fun WalletManager.isDemoWallet(): Boolean = DemoHelper.isDemoCardId(wallet.cardId)
