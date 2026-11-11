package com.tangem.features.tangempay

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.EventMessage
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach

internal abstract class TangemPayModelTestBase {

    protected val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    protected val router: Router = mockk(relaxed = true)
    protected val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private val modelsToDestroy = mutableListOf<Model>()

    @AfterEach
    fun destroyModels() {
        modelsToDestroy.forEach(Model::onDestroy)
        modelsToDestroy.clear()
    }

    protected fun <M : Model> M.destroyAfterTest(): M = also(modelsToDestroy::add)

    protected fun lastBottomSheet(): MessageBottomSheetUM {
        val message = slot<EventMessage>()
        verify { uiMessageSender.send(capture(message)) }
        return (message.captured as BottomSheetMessage).messageBottomSheetUM
    }

    protected fun lastMessageTitle(): TextReference? = lastBottomSheet().elements
        .filterIsInstance<MessageBottomSheetUM.InfoBlock>()
        .firstNotNullOfOrNull { it.title }

    protected fun lastMessageButtons(): List<MessageBottomSheetUM.Button> =
        lastBottomSheet().elements.filterIsInstance<MessageBottomSheetUM.Button>()

    protected fun closeLastMessage() {
        clickLastMessageButton { first() }
    }

    protected fun retryLastMessage() {
        clickLastMessageButton { last() }
    }

    private fun clickLastMessageButton(pick: List<MessageBottomSheetUM.Button>.() -> MessageBottomSheetUM.Button) {
        val sheet = lastBottomSheet()
        sheet.elements.filterIsInstance<MessageBottomSheetUM.Button>().pick().onClick?.invoke(sheet.closeScope)
    }
}