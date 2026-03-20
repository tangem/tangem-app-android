package com.tangem.common.test

import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.CoroutineContext

class TestAppCoroutineScope(override val coroutineContext: CoroutineContext = Dispatchers.Unconfined) : AppCoroutineScope {

    constructor(testScope: TestScope) : this(testScope.coroutineContext)
}