package com.tangem.plugin.configuration.model

/**
 * Build config field
 *
 * @property type  field type
 * @property name  field name
 * @property value field value
 */
internal sealed class BuildConfigField(val type: String, val name: String, val value: String) {

    class Environment(value: String) : BuildConfigField(
        type = "String",
        name = "ENVIRONMENT",
        value = "\"$value\"",
    )

    // TODO remove
    class TestActionEnabled(isEnabled: Boolean) : BuildConfigField(
        type = "Boolean",
        name = "TEST_ACTION_ENABLED",
        value = isEnabled.toString(),
    )

    class LogEnabled(isEnabled: Boolean) : BuildConfigField(
        type = "Boolean",
        name = "LOG_ENABLED",
        value = isEnabled.toString(),
    )

    class TesterMenuAvailability(isEnabled: Boolean) : BuildConfigField(
        type = "Boolean",
        name = "TESTER_MENU_ENABLED",
        value = isEnabled.toString(),
    )

    class MockDataSource(isEnabled: Boolean) : BuildConfigField(
        type = "Boolean",
        name = "MOCK_DATA_SOURCE",
        value = isEnabled.toString(),
    )
}