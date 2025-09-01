package com.tangem.utils.buildConfig

interface AppConfigurationProvider {
    fun isDebug(): Boolean
    fun isHuawei(): Boolean
}