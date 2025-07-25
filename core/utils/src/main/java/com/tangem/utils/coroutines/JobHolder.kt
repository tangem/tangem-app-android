package com.tangem.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Job holder. It is automatically finished old job if new one is started
 *
[REDACTED_AUTHOR]
 */
class JobHolder {

    val isActive: Boolean
        get() = job?.isActive ?: false

    private var job: Job? = null

    /** Update current [JobHolder.job] and return new [job] */
    fun update(job: Job): Job {
        this.job?.cancel()
        this.job = job
        return job
    }

    /** Cancel current [job] */
    fun cancel() {
        job?.cancel()
        job = null
    }

    fun isEmpty() = job == null
}

fun Job.saveIn(jobHolder: JobHolder): Job = jobHolder.update(job = this)

suspend fun Job.saveInAndJoin(jobHolder: JobHolder) = saveIn(jobHolder).join()

fun CoroutineScope.withDebounce(jobHolder: JobHolder, timeMillis: Long = 800L, function: () -> Unit) {
    launch {
        delay(timeMillis = timeMillis)

        function()
    }
        .saveIn(jobHolder)
}