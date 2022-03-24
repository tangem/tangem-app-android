package com.tangem.tap.features.demo

import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.domain.tasks.product.ScanResponse

/**
[REDACTED_AUTHOR]
 */
fun ScanResponse.isDemoCard(): Boolean = DemoHelper.isDemoCardId(card.cardId)
fun WalletManager.isDemoWallet(): Boolean = DemoHelper.isDemoCardId(wallet.cardId)