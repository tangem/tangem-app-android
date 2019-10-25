package com.tangem

import java.util.concurrent.Executor
import java.util.concurrent.Executors


object AppExecutors {

    private val diskIO = Executors.newSingleThreadExecutor()
    private val networkIO = Executors.newFixedThreadPool(3)

    fun diskIO(): Executor {
        return diskIO
    }

    fun networkIO(): Executor {
        return networkIO
    }

}