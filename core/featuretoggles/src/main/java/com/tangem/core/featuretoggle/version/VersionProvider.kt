package com.tangem.core.featuretoggle.version

/** Application version provider */
internal interface VersionProvider {

    /** Get application version */
    fun get(): String?
}
