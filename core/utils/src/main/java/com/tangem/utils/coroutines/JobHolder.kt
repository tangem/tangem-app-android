package com.tangem.utils.coroutines

import kotlinx.coroutines.Job

/**
 * Job holder. It is automatically finished old job if new one is started
 *
 * @author Andrew Khokhlov on 27/07/2023
 */
class JobHolder {

    private var job: Job? = null

    /** Update current [job] */
    fun update(job: Job?) {
        this.job?.cancel()
        this.job = job
    }

    /** Cancel current [job] */
    fun cancel() {
        job?.cancel()
    }
}

fun Job.saveIn(jobHolder: JobHolder) = jobHolder.update(job = this)
