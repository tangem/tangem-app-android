package com.tangem.utils.coroutines

import kotlinx.coroutines.Job

/**
 * Job holder. It is automatically finished old job if new one is started
 *
* [REDACTED_AUTHOR]
 */
class JobHolder {

    private var job: Job? = null

    /** Update current [job] */
    fun update(job: Job?) {
        this.job?.cancel()
        this.job = job
    }
}

fun Job.saveIn(jobHolder: JobHolder) = jobHolder.update(job = this)
