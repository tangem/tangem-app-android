typealias ConfigFieldBuilder = (String, String, String) -> Unit

class BuildConfigFieldFactory(
    private val fields: List<Field>,
    private val builder: ConfigFieldBuilder,
) {
    fun create() {
        fields.forEach { field -> builder(field.type, field.name, field.value) }
    }
}

/**
 * Build config field
 *
 * @property type  field type
 * @property name  field name
 * @property value field value
 */
sealed class Field(val type: String, val name: String, val value: String) {

    class Environment(value: String) : Field(type = "String", name = "ENVIRONMENT", value = "\"$value\"")

    class TestActionEnabled(isEnabled: Boolean) : Field(
        type = "Boolean",
        name = "TEST_ACTION_ENABLED",
        value = isEnabled.toString(),
    )

    class LogEnabled(isEnabled: Boolean) : Field(type = "Boolean", name = "LOG_ENABLED", value = isEnabled.toString())

    class TesterMenuAvailability(isEnabled: Boolean) : Field(
        type = "Boolean",
        name = "TESTER_MENU_ENABLED",
        value = isEnabled.toString(),
    )
}