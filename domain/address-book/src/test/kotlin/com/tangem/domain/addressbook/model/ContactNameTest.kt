package com.tangem.domain.addressbook.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactNameTest {

    @Test
    fun `valid name is accepted and trimmed`() {
        val result = ContactName("  Alice 1  ")

        assertThat(result.getOrNull()?.value).isEqualTo("Alice 1")
    }

    @Test
    fun `single character name is accepted`() {
        assertThat(ContactName("A").isRight()).isTrue()
    }

    @Test
    fun `name of max length is accepted`() {
        val name = "a".repeat(ContactName.MAX_LENGTH)

        assertThat(ContactName(name).isRight()).isTrue()
    }

    @Test
    fun `blank name is rejected as Empty`() {
        assertThat(ContactName("   ").leftOrNull()).isEqualTo(ContactName.Error.Empty)
    }

    @Test
    fun `name exceeding max length is rejected`() {
        val name = "a".repeat(ContactName.MAX_LENGTH + 1)

        assertThat(ContactName(name).leftOrNull()).isEqualTo(ContactName.Error.ExceedsMaxLength)
    }

    @Test
    fun `emoji is rejected`() {
        assertThat(ContactName("Alice 😀").leftOrNull()).isEqualTo(ContactName.Error.InvalidCharacters)
    }

    @Test
    fun `new line is rejected`() {
        assertThat(ContactName("Ali\nce").leftOrNull()).isEqualTo(ContactName.Error.InvalidCharacters)
    }

    @Test
    fun `tab is rejected`() {
        assertThat(ContactName("Ali\tce").leftOrNull()).isEqualTo(ContactName.Error.InvalidCharacters)
    }

    @Test
    fun `html script is rejected`() {
        assertThat(ContactName("<script>alert(1)</script>").leftOrNull())
            .isEqualTo(ContactName.Error.InvalidCharacters)
    }

    @Test
    fun `special symbols are rejected`() {
        assertThat(ContactName("Alice@!").leftOrNull()).isEqualTo(ContactName.Error.InvalidCharacters)
    }
}