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
    fun `simple emoji is accepted`() {
        assertThat(ContactName("Alice 😀").isRight()).isTrue()
    }

    @Test
    fun `emoji-only name is accepted`() {
        assertThat(ContactName("😀").isRight()).isTrue()
    }

    @Test
    fun `flag emoji is accepted`() {
        assertThat(ContactName("Team 🇺🇸").isRight()).isTrue()
    }

    @Test
    fun `zwj emoji sequence is accepted`() {
        assertThat(ContactName("Family 👨‍👩‍👧").isRight()).isTrue()
    }

    @Test
    fun `emoji with variation selector is accepted`() {
        assertThat(ContactName("Love ❤️").isRight()).isTrue()
    }

    @Test
    fun `non-latin letters are accepted`() {
        assertThat(ContactName("Алёша 大阪").isRight()).isTrue()
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
    fun `zero-width space is rejected`() {
        assertThat(ContactName("Ali\u200Bce").leftOrNull()).isEqualTo(ContactName.Error.InvalidCharacters)
    }

    @Test
    fun `non-breaking space is rejected`() {
        assertThat(ContactName("Ali\u00A0ce").leftOrNull()).isEqualTo(ContactName.Error.InvalidCharacters)
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