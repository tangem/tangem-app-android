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
        override val value: Boolean?
) : BaseConfigModel<Boolean>

class ConfigValueModel(
        override val name: String,
        override val value: String
) : BaseConfigModel<String>

class ConfigModel(val features: List<FeatureModel>?, val configValues: List<ConfigValueModel>?)

fun ConfigModel.toFeatures(type: ConfigType): MutableMap<String, Feature> {
    return features?.map { AppFeature(type, it, ConditionsFactory.create(it.name)) }
            ?.associateBy { it.name }
            ?.toMutableMap()
            ?: mutableMapOf()
}

fun ConfigModel.toConfigValues(): MutableMap<String, ConfigurationValue> {
    return configValues?.map { ConfigurationValue(it.name, it.value) }
            ?.associateBy { it.name }
            ?.toMutableMap() ?: mutableMapOf()
}

