package com.tangem.grow.datasource.polymarket.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

internal class PolymarketCategoriesResponseTest {

    private val adapter = Moshi.Builder().build().adapter(PolymarketCategoriesResponse::class.java)

    @Test
    fun `GIVEN nested categories from BFF WHEN decoded THEN children and filters are retained`() {
        // Arrange
        val json = """
            {"categories":[{"id":6,"label":"Sports","icon":"sports","hasSeries":true,
            "filters":["games","props"],"subcategories":[
            {"id":14,"label":"prediction_category_soccer","icon":null,
            "filters":["games","props","future"],"subcategories":[]}]}]}
        """.trimIndent()

        // Act
        val category = requireNotNull(adapter.fromJson(json)).categories.single()
        val child = requireNotNull(category.subcategories).single()

        // Assert
        assertThat(category.filters).containsExactly("games", "props").inOrder()
        assertThat(child.id).isEqualTo(14)
        assertThat(child.label).isEqualTo("prediction_category_soccer")
        assertThat(child.filters).containsExactly("games", "props", "future").inOrder()
        assertThat(child.subcategories).isEmpty()
    }

    @Test
    fun `GIVEN legacy and nullable fields WHEN decoded THEN both category shapes remain accepted`() {
        // Arrange
        val json = """
            {"categories":[{"id":2,"label":"Politics","icon":null},
            {"id":6,"label":"Sports","icon":"sports","filters":null,"subcategories":null}]}
        """.trimIndent()

        // Act
        val categories = requireNotNull(adapter.fromJson(json)).categories

        // Assert
        assertThat(categories.map { it.filters }).containsExactly(null, null)
        assertThat(categories.map { it.subcategories }).containsExactly(null, null)
    }
}