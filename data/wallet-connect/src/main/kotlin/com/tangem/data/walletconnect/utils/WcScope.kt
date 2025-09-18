package com.tangem.data.walletconnect.utils

import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

internal class WcScope(
    dispatchers: CoroutineDispatcherProvider,
) : CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob() + dispatchers.io
}