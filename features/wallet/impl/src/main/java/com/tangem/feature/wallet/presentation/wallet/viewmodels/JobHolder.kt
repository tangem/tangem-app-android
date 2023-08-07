package com.tangem.feature.wallet.presentation.wallet.viewmodels

import kotlinx.coroutines.Job

/**
 * Job holder. It is automatically finished old job if new one is started
 *
 * @author Andrew Khokhlov on 27/07/2023
 */
internal class JobHolder {

    private var job: Job? = null

    /** Update current job */
    fun update(job: Job) {
        this.job?.cancel()
        this.job = job
    }
}

internal fun Job.saveIn(jobHolder: JobHolder) = jobHolder.update(job = this)
