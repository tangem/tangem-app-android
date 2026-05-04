package com.tangem.tap.domain.scanCard

import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.card.ScanFailsCounter
import com.tangem.domain.card.ScanFailsRequester
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultScanFailsCounter @Inject constructor(
    private val scanFailsRequester: ScanFailsRequester,
    private val appScope: AppCoroutineScope,
) : ScanFailsCounter {

    private var counter: Int = 0

    override fun reset() {
        counter = 0
    }

    override fun onScanFailure(isUserCancelled: Boolean, source: AnalyticsParam.ScreensSources) {
        if (isUserCancelled) {
            counter++
            if (counter >= THRESHOLD) {
                appScope.launch { scanFailsRequester.show(source) }
            }
        } else {
            counter = 0
        }
    }

    private companion object {
        const val THRESHOLD = 2
    }
}