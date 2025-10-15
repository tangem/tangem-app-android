package com.tangem.datasource.utils

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

// --- DTO ---
@SerializeNulls
@JsonClass(generateAdapter = true)
data class UserWithNulls(val id: String?, val name: String?)

@JsonClass(generateAdapter = true)
data class UserWithoutNulls(val id: String?, val name: String?)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SerializeNullsFactoryTest {

    private val moshi = Moshi.Builder()
        .add(SerializeNullsFactory)
        .build()

    @Test
    fun `should serialize nulls for annotated class`() {
        val adapter = moshi.adapter(UserWithNulls::class.java)

        val json = adapter.toJson(UserWithNulls(id = null, name = "John"))

        assertThat(json).isEqualTo("""{"id":null,"name":"John"}""")
    }

    @Test
    fun `should skip nulls for non-annotated class`() {
        val adapter = moshi.adapter(UserWithoutNulls::class.java)

        val json = adapter.toJson(UserWithoutNulls(id = null, name = "John"))

        assertThat(json).isEqualTo("""{"name":"John"}""")
    }

    @Test
    fun `should deserialize annotated class correctly`() {
        val adapter = moshi.adapter(UserWithNulls::class.java)

        val json = """{"id":null,"name":"Jane"}"""
        val result = adapter.fromJson(json)

        assertThat(result).isEqualTo(UserWithNulls(id = null, name = "Jane"))
    }
}