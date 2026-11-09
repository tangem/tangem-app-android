package com.tangem.core.local.moshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.lang.reflect.Type

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersistedModelGuardTest {

    @Test
    fun `GIVEN strict guard WHEN adapter of class without codegen is created THEN fails naming the class`() {
        // Arrange
        val moshi = createMoshi(isStrict = true)

        // Act
        val error = runCatching { moshi.adapter(ReflectiveModel::class.java) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().contains(ReflectiveModel::class.java.name)
    }

    @Test
    fun `GIVEN strict guard WHEN class annotated for codegen has no generated adapter THEN fails naming the class`() {
        // Arrange
        val moshi = createMoshi(isStrict = true)

        // Act
        val error = runCatching { moshi.adapter(ModelWithoutGeneratedAdapter::class.java) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().contains(ModelWithoutGeneratedAdapter::class.java.name)
    }

    @Test
    fun `GIVEN strict guard WHEN codegen class has a field without codegen THEN fails naming the field class`() {
        // Arrange
        val moshi = createMoshi(isStrict = true)

        // Act
        val error = runCatching { moshi.adapter(CodegenModelWithReflectiveField::class.java) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().contains(ReflectiveModel::class.java.name)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN strict guard WHEN adapter of type not read by reflection is created THEN succeeds`(type: Type) {
        // Arrange
        val moshi = createMoshi(isStrict = true)

        // Act
        val error = runCatching { moshi.adapter<Any>(type) }.exceptionOrNull()

        // Assert
        assertThat(error).isNull()
    }

    @Test
    fun `GIVEN lenient guard WHEN adapter of class without codegen is created THEN falls back to reflection`() {
        // Arrange
        val moshi = createMoshi(isStrict = false)

        // Act
        val json = moshi.adapter(ReflectiveModel::class.java).toJson(ReflectiveModel(id = "1"))

        // Assert
        assertThat(json).isEqualTo("""{"id":"1"}""")
    }

    private fun provideTestModels(): List<Type> = listOf(
        CodegenModel::class.java,
        Mode::class.java,
        String::class.java,
    )

    private fun createMoshi(isStrict: Boolean): Moshi = Moshi.Builder()
        .add(PersistedModelGuard(isStrict = isStrict))
        .addLast(KotlinJsonAdapterFactory())
        .build()
}

private data class ReflectiveModel(val id: String)

private enum class Mode { DEFAULT }

@JsonClass(generateAdapter = true)
private data class CodegenModel(val id: String)

@JsonClass(generateAdapter = true)
private data class ModelWithoutGeneratedAdapter(val id: String)

@JsonClass(generateAdapter = true)
private data class CodegenModelWithReflectiveField(val nested: ReflectiveModel)

/** Stands in for the adapter Moshi codegen would generate */
private class CodegenModelJsonAdapter : JsonAdapter<CodegenModel>() {
    override fun fromJson(reader: JsonReader): CodegenModel = error("Not used")
    override fun toJson(writer: JsonWriter, value: CodegenModel?): Unit = error("Not used")
}

/** Like a generated adapter, resolves the adapters of its properties when it is created */
private class CodegenModelWithReflectiveFieldJsonAdapter(moshi: Moshi) : JsonAdapter<CodegenModelWithReflectiveField>() {
    init {
        moshi.adapter(ReflectiveModel::class.java)
    }

    override fun fromJson(reader: JsonReader): CodegenModelWithReflectiveField = error("Not used")
    override fun toJson(writer: JsonWriter, value: CodegenModelWithReflectiveField?): Unit = error("Not used")
}