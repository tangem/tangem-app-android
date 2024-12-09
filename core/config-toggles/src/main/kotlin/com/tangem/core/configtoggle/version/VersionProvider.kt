package com.tangem.core.configtoggle.version

/** Application version provider */
internal interface VersionProvider {

    /** Get application version */
    fun get(): String?
}