typealias ConfigFieldBuilder = (String, String, String) -> Unit

class BuildConfigFieldFactory(
    private val fields: List<Field>,
    private val builder: ConfigFieldBuilder,
) {
    fun create() {
        fields.forEach { builder(it.type, it.name, it.value) }
    }
}

sealed class Field(
    val type: String,
    val name: String,
    val value: String,
) {
    open class StringField(name: String, value: String) : Field("String", name, "\"$value\"")
    open class BooleanField(name: String, value: Boolean) : Field("Boolean", name, value.toString())

    class Environment(value: String) : StringField("ENVIRONMENT", value)
    class TestActionEnabled(isEnabled: Boolean) : BooleanField("TEST_ACTION_ENABLED", isEnabled)
    class LogEnabled(isEnabled: Boolean) : BooleanField("LOG_ENABLED", isEnabled)
}