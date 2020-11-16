package com.tangem.tap.domain.config

/**
[REDACTED_AUTHOR]
 */

interface Feature {
    val name: String
    val type: ConfigType
    fun isActive(): Boolean
    fun reset()
    fun updateCondition(condition: Condition?)
    fun hasCondition(): Boolean

    companion object {
        const val usePayId = "usePayId"
        const val payIdIsEnabled = "payIdIsEnabled"
        const val useTopUp = "useTopUp"
    }
}

class AppFeature(
        override val type: ConfigType,
        private val featureModel: FeatureModel,
        private var condition: Condition?
) : Feature {
    override val name: String = featureModel.name

    override fun isActive(): Boolean {
        return if (condition == null) featureModel.value ?: false
        else condition!!.isMet()
    }

    override fun reset() {
        condition = null
    }

    override fun updateCondition(condition: Condition?) {
        if (type == ConfigType.Local) this.condition = condition
    }

    override fun hasCondition(): Boolean {
        return condition != null
    }
}

data class ConfigurationValue(val name: String, val value: String) {
    companion object {
        const val coinMarketCapKey = "coinMarketCapKey"
        const val moonPayApiKey = "moonPayApiKey"
        const val moonPayApiSecretKey = "moonPayApiSecretKey"
    }
}