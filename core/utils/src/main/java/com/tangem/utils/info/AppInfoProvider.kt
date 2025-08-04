package com.tangem.utils.info

interface AppInfoProvider {
    val platform: String
    val device: String
    val osVersion: String
    val language: String
    val timezone: String
    val appVersion: String
    val isHuaweiDevice: Boolean
}