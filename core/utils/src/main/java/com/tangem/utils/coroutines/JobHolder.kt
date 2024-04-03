package com.tangem.utils.coroutines

import kotlinx.coroutines.Job

/**
 * Job holder. It is automatically finished old job if new one is started
 *
 * @author Andrew Khokhlov on 27/07/2023
 */
class JobHolder {

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
    }
}

fun Job.saveIn(jobHolder: JobHolder): Job = jobHolder.update(job = this)
