package com.tangem.core.media

/** Stands in for the main thread the teardown is handed to; runs it on the spot unless asked to hold it. */
internal class FakeTeardownScheduler(private val runsImmediately: Boolean = true) : VideoTeardownScheduler {

    var scheduled: (() -> Unit)? = null
        private set

    override fun schedule(action: () -> Unit) {
        scheduled = action
        if (runsImmediately) action()
    }

    override fun cancel() {
        scheduled = null
    }
}