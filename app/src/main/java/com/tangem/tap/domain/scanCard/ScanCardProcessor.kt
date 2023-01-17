package com.tangem.tap.domain.scanCard

import com.tangem.blockchain.common.Blockchain
import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.tap.backupService
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.domain.scanCard.chains.AnalyticsChain
import com.tangem.tap.domain.scanCard.chains.CardScannedChain
import com.tangem.tap.domain.scanCard.chains.DisclaimerChainData
import com.tangem.tap.domain.scanCard.chains.SaltPayCardScannedChain
import com.tangem.tap.domain.scanCard.chains.SaltPayCheckUnfinishedBackupChain
import com.tangem.tap.domain.scanCard.chains.ScanChain
import com.tangem.tap.domain.scanCard.chains.ShowDisclaimerChain
import com.tangem.tap.domain.scanCard.chains.UpdateConfigManagerChain
import com.tangem.tap.tangemSdkManager

/**
 * Created by Anton Zhilenkov on 05.01.2023.
 */
object ScanCardProcessor {

    fun scan(
        cardScannedEvent: AnalyticsEvent? = null,
        disclaimerChainData: DisclaimerChainData = DisclaimerChainData(fromScreen = AppScreen.Home),
        onScanStateChange: suspend (scanInProgress: Boolean) -> Unit = {},
        onWalletNotCreated: suspend (() -> Unit) = {},
    ): ScanCardChainProcessor {
        val processor = ScanCardChainProcessor()
        processor.addChain(ScanChain(tangemSdkManager, onScanStateChange = onScanStateChange))
        processor.addChain(SaltPayCheckUnfinishedBackupChain(backupService))
        processor.addChain(ShowDisclaimerChain(disclaimerChainData))
        processor.addChain(AnalyticsChain(cardScannedEvent))
        processor.addChain(UpdateConfigManagerChain())
        processor.addChain(SaltPayCardScannedChain(onWalletNotCreated))
        processor.addChain(CardScannedChain(onWalletNotCreated))

        return processor
    }

    fun deriveBlockchains(
        cardId: String? = null,
        additionalBlockchainsToDerive: Collection<Blockchain>? = null,
    ): ScanCardChainProcessor {
        val processor = ScanCardChainProcessor()
        processor.addChain(ScanChain(tangemSdkManager, cardId, additionalBlockchainsToDerive))

        return processor
    }
}
