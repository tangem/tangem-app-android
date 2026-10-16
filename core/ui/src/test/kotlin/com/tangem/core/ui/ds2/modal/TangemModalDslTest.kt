package com.tangem.core.ui.ds2.modal

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.stringReference
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TangemModalDslTest {

    @Nested
    inner class Status {

        @Test
        fun `GIVEN only required fields WHEN tangemModalStatusUM THEN optional fields keep defaults`() {
            // Act
            val actual = tangemModalStatusUM {
                variant = TangemModalStatus.Variant.Success
                title = TITLE
            }

            // Assert
            val expected = TangemModalContentUM.Status(
                variant = TangemModalStatus.Variant.Success,
                title = TITLE,
                description = null,
                titleStyle = TangemModalStatus.TitleStyle.Status,
                asset = TangemModalContentUM.Status.Asset.Default,
                badge = null,
                details = persistentListOf(),
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN every block WHEN tangemModalStatusUM THEN model carries them with details in order`() {
            // Act
            val actual = tangemModalStatusUM {
                variant = TangemModalStatus.Variant.Error
                title = TITLE
                description = DESCRIPTION
                titleStyle = TangemModalStatus.TitleStyle.Amount
                glyph(res = GLYPH_RES, isTinted = false)
                badge(text = BADGE_TEXT, status = TangemBadge.Status.Error)
                detail(title = stringReference(value = "Fee"), value = stringReference(value = "$5.00"))
                detail(title = stringReference(value = "Total"), value = stringReference(value = "$171.84"))
            }

            // Assert
            val expected = TangemModalContentUM.Status(
                variant = TangemModalStatus.Variant.Error,
                title = TITLE,
                description = DESCRIPTION,
                titleStyle = TangemModalStatus.TitleStyle.Amount,
                asset = TangemModalContentUM.Status.Asset.GlyphRes(res = GLYPH_RES, isTinted = false),
                badge = TangemBadgeUM(text = BADGE_TEXT, status = TangemBadge.Status.Error),
                details = persistentListOf(
                    TangemModalContentUM.Status.Detail(
                        title = stringReference(value = "Fee"),
                        value = stringReference(value = "$5.00"),
                    ),
                    TangemModalContentUM.Status.Detail(
                        title = stringReference(value = "Total"),
                        value = stringReference(value = "$171.84"),
                    ),
                ),
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN no title WHEN tangemModalStatusUM THEN throws`() {
            // Act
            val actual = runCatching {
                tangemModalStatusUM { variant = TangemModalStatus.Variant.Info }
            }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(UninitializedPropertyAccessException::class.java)
        }
    }

    @Nested
    inner class Informer {

        @Test
        fun `GIVEN text WHEN tangemModalInformerUM THEN builds Plain`() {
            // Act
            val actual = tangemModalInformerUM { text = DESCRIPTION }

            // Assert
            assertThat(actual).isEqualTo(TangemModalContentUM.Informer.Plain(text = DESCRIPTION))
        }

        @Test
        fun `GIVEN items WHEN tangemModalInformerUM THEN builds Rich with items in order`() {
            // Act
            val actual = tangemModalInformerUM {
                item(title = TITLE, description = DESCRIPTION)
                item(title = stringReference(value = "Second"), description = stringReference(value = "Next"))
            }

            // Assert
            val expected = TangemModalContentUM.Informer.Rich(
                items = persistentListOf(
                    TangemModalContentUM.Informer.Item(title = TITLE, description = DESCRIPTION),
                    TangemModalContentUM.Informer.Item(
                        title = stringReference(value = "Second"),
                        description = stringReference(value = "Next"),
                    ),
                ),
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN text and items WHEN tangemModalInformerUM THEN throws IllegalStateException`() {
            // Act
            val actual = runCatching {
                tangemModalInformerUM {
                    text = DESCRIPTION
                    item(title = TITLE, description = DESCRIPTION)
                }
            }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Nested
    inner class Menu {

        @Test
        fun `GIVEN items WHEN tangemModalMenuUM THEN builds items in order`() {
            // Arrange
            val clickAction: TangemModalActionScope.() -> Unit = {}
            val avatar = TangemAvatar.UiState.User(initial = 'A', color = TangemAvatar.FillColor.DarkBlue)

            // Act
            val actual = tangemModalMenuUM {
                item {
                    title = TITLE
                    subtitle = DESCRIPTION
                    this.avatar = avatar
                    onClick(clickAction)
                }
                item {
                    title = stringReference(value = "Second")
                    onClick = clickAction
                }
            }

            // Assert
            val expected = TangemModalContentUM.Menu(
                items = persistentListOf(
                    TangemModalContentUM.Menu.Item(
                        title = TITLE,
                        subtitle = DESCRIPTION,
                        avatar = avatar,
                        onClick = clickAction,
                    ),
                    TangemModalContentUM.Menu.Item(title = stringReference(value = "Second"), onClick = clickAction),
                ),
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN item without action WHEN clicked THEN modal is dismissed`() {
            // Arrange
            var dismissCount = 0
            val actionScope = TangemModalActionScope { dismissCount++ }
            val menu = tangemModalMenuUM { item { title = TITLE } }

            // Act
            menu.items.single().onClick(actionScope)

            // Assert
            assertThat(dismissCount).isEqualTo(1)
        }
    }

    private companion object {
        val TITLE = stringReference(value = "Title")
        val DESCRIPTION = stringReference(value = "Description")
        val BADGE_TEXT = stringReference(value = "Badge")
        const val GLYPH_RES = 1
    }
}