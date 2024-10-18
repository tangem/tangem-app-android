package com.tangem.core.toggle.version

/** Application version provider */
internal interface VersionProvider {

    /** Get application version */
    fun get(): String?
}
