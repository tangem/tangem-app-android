package com.tangem.tap.features.scanfails

import com.tangem.domain.card.ScanFailsRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanFailsRequesterProxy @Inject constructor() : ScanFailsRequester {

    val componentRequester = MutableStateFlow<ScanFailsRequester?>(null)

    override suspend fun show(source: ScanFailsRequester.Source): ScanFailsRequester.Result {
        return withTimeout(timeMillis = 1000) {
            componentRequester.filterNotNull().first()
        }.show(source)
    }
}