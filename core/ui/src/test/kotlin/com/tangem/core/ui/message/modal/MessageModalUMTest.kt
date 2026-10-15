package com.tangem.core.ui.message.modal

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.ds2.modal.tangemModalInformerUM
import com.tangem.core.ui.ds2.modal.tangemModalStatusUM
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.stringReference
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class MessageModalUMTest {

    @Nested
    inner class Build {

        @Test
        fun `GIVEN every block WHEN messageModalUM THEN model keeps buttons in declaration order`() {
            // Arrange
            val primaryClick: TangemModalActionScope.() -> Unit = {}
            val secondaryClick: TangemModalActionScope.() -> Unit = {}
            val dismissCallback = {}

            // Act
            val actual = messageModalUM {
                title = MODAL_TITLE
                status {
                    variant = TangemModalStatus.Variant.Warning
                    title = TITLE
                }
                primaryButton {
                    text = PRIMARY_TEXT
                    onClick(primaryClick)
                }
                secondaryButton {
                    text = SECONDARY_TEXT
                    onClick = secondaryClick
                }
                onDismiss(dismissCallback)
            }

            // Assert
            val expected = MessageModalUM(
                content = tangemModalStatusUM {
                    variant = TangemModalStatus.Variant.Warning
                    title = TITLE
                },
                title = MODAL_TITLE,
                buttons = persistentListOf(
                    MessageModalUM.Button(
                        text = PRIMARY_TEXT,
                        variant = TangemButton.Variant.Primary,
                        onClick = primaryClick,
                    ),
                    MessageModalUM.Button(
                        text = SECONDARY_TEXT,
                        variant = TangemButton.Variant.Secondary,
                        onClick = secondaryClick,
                    ),
                ),
                onDismiss = dismissCallback,
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN informer block WHEN messageModalUM THEN content is the informer`() {
            // Act
            val actual = messageModalUM { informer { text = TITLE } }

            // Assert
            assertThat(actual.content).isEqualTo(tangemModalInformerUM { text = TITLE })
        }

        @Test
        fun `GIVEN button without action WHEN clicked THEN modal is dismissed`() {
            // Arrange
            var dismissCount = 0
            val actionScope = TangemModalActionScope { dismissCount++ }
            val model = messageModalUM {
                informer { text = TITLE }
                primaryButton { text = PRIMARY_TEXT }
            }

            // Act
            model.buttons.single().onClick(actionScope)

            // Assert
            assertThat(dismissCount).isEqualTo(1)
        }

        @Test
        fun `GIVEN no content WHEN messageModalUM THEN throws IllegalStateException`() {
            // Act
            val actual = runCatching { messageModalUM { primaryButton { text = PRIMARY_TEXT } } }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `GIVEN two content blocks WHEN messageModalUM THEN throws IllegalStateException`() {
            // Act
            val actual = runCatching {
                messageModalUM {
                    informer { text = TITLE }
                    menu { item { title = TITLE } }
                }
            }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Nested
    @Suppress("DEPRECATION")
    inner class LegacyGrammar {

        @Test
        fun `GIVEN typed icon infoBlock WHEN messageModalUM THEN maps to tinted status of the matching variant`() {
            // Act
            val actual = messageModalUM {
                infoBlock {
                    icon(ICON_RES) { type = MessageBottomSheetUM.Icon.Type.Attention }
                    title = TITLE
                    body = BODY
                    chip(text = CHIP_TEXT) { type = MessageBottomSheetUM.Chip.Type.Warning }
                }
            }

            // Assert
            val expected = TangemModalContentUM.Status(
                variant = TangemModalStatus.Variant.Warning,
                title = TITLE,
                description = BODY,
                asset = TangemModalContentUM.Status.Asset.GlyphRes(res = ICON_RES, isTinted = true),
                badge = TangemBadgeUM(text = CHIP_TEXT, status = TangemBadge.Status.Warning),
            )
            assertThat(actual.content).isEqualTo(expected)
        }

        @Test
        fun `GIVEN untyped icon infoBlock WHEN messageModalUM THEN keeps the icon colors on a neutral status`() {
            // Act
            val actual = messageModalUM {
                infoBlock {
                    icon(ICON_RES)
                    title = TITLE
                }
            }

            // Assert
            val expected = TangemModalContentUM.Status(
                variant = TangemModalStatus.Variant.Neutral,
                title = TITLE,
                asset = TangemModalContentUM.Status.Asset.GlyphRes(res = ICON_RES, isTinted = false),
            )
            assertThat(actual.content).isEqualTo(expected)
        }

        @Test
        fun `GIVEN onClick assigned with closeBs WHEN clicked THEN modal is dismissed`() {
            // Arrange
            var dismissCount = 0
            val actionScope = TangemModalActionScope { dismissCount++ }
            val model = messageModalUM {
                infoBlock { title = TITLE }
                primaryButton {
                    text = PRIMARY_TEXT
                    onClick = { closeBs() }
                }
            }

            // Act
            model.buttons.single().onClick(actionScope)

            // Assert
            assertThat(dismissCount).isEqualTo(1)
        }

        @Test
        fun `GIVEN icon set before and after text WHEN messageModalUM THEN icon goes to start and end`() {
            // Act
            val actual = messageModalUM {
                infoBlock { title = TITLE }
                primaryButton {
                    icon = ICON_RES
                    text = PRIMARY_TEXT
                }
                secondaryButton {
                    text = SECONDARY_TEXT
                    icon = ICON_RES
                }
            }

            // Assert
            val iconPlacement = actual.buttons.map { button -> button.iconStart.iconRes() to button.iconEnd.iconRes() }
            assertThat(iconPlacement).containsExactly(ICON_RES to null, null to ICON_RES).inOrder()
        }

        // TangemIconUM.Icon carries a lambda tint, so two equal icons never compare equal.
        private fun TangemIconUM?.iconRes(): Int? = (this as? TangemIconUM.Icon)?.icon?.leftOrNull()

        @Test
        fun `GIVEN onDismissRequest assigned WHEN messageModalUM THEN it becomes onDismiss`() {
            // Arrange
            val dismissCallback = {}

            // Act
            val actual = messageModalUM {
                infoBlock { title = TITLE }
                onDismissRequest = dismissCallback
            }

            // Assert
            assertThat(actual.onDismiss).isSameInstanceAs(dismissCallback)
        }
    }

    private companion object {
        val TITLE = stringReference(value = "Title")
        val MODAL_TITLE = stringReference(value = "Modal title")
        val BODY = stringReference(value = "Body")
        val CHIP_TEXT = stringReference(value = "Chip")
        val PRIMARY_TEXT = stringReference(value = "Got it")
        val SECONDARY_TEXT = stringReference(value = "Later")
        const val ICON_RES = 1
    }
}