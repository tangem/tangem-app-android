package com.tangem.plugin.configuration.utils

import com.tangem.plugin.configuration.model.BuildConfigField

private typealias ConfigFieldBuilder = (String, String, String) -> Unit

internal class BuildConfigFieldFactory(
    private val fields: List<BuildConfigField>,
    private val builder: ConfigFieldBuilder,
) {
    fun create() {
        fields.forEach { field -> builder(field.type, field.name, field.value) }
    }
}