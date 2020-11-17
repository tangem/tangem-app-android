package com.tangem.tap.domain.config

/**
[REDACTED_AUTHOR]
 */

interface BaseConfigModel<V> {
    val name: String
    val value: V?
}

class FeatureModel(
        override val name: String,
        override val value: Boolean
) : BaseConfigModel<Boolean>

class ConfigValueModel(
        override val name: String,
        override val value: String
) : BaseConfigModel<String>

class ConfigModel(val features: List<FeatureModel>?, val configValues: List<ConfigValueModel>?) {
    companion object {
        fun empty(): ConfigModel = ConfigModel(listOf(), listOf())
    }
}