package com.tangem.features.addressbook.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.features.addressbook.AddressBookComponent
import com.tangem.features.addressbook.list.AddressBookListComponent
import com.tangem.features.addressbook.editcontact.EditContactComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddressBookComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: AddressBookComponent.Params,
    private val addressBookListComponentFactory: AddressBookListComponent.Factory,
    private val editContactComponentFactory: EditContactComponent.Factory,
) : AddressBookComponent, AppComponentContext by context {

    private val navigation = StackNavigation<AddressBookRoute>()

    private val contentStack = childStack(
        key = "address_book_stack",
        source = navigation,
        serializer = AddressBookRoute.serializer(),
        initialConfiguration = AddressBookRoute.List,
        handleBackButton = false,
        childFactory = ::screenChild,
    )

    @Suppress("ReusedModifierInstance")
    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by contentStack.subscribeAsState()

        Children(stack = childStack, animation = stackAnimation()) { child ->
            child.instance.Content(modifier = modifier)
        }
    }

    private fun screenChild(config: AddressBookRoute, componentContext: ComponentContext): ComposableContentComponent =
        when (config) {
            AddressBookRoute.List -> addressBookListComponentFactory.create(
                context = childByContext(componentContext),
                params = AddressBookListComponent.Params(
                    onContactClick = { contactId ->
                        navigation.pushNew(AddressBookRoute.EditContact(contactId))
                    },
                    onAddContactClick = { navigation.pushNew(AddressBookRoute.EditContact()) },
                ),
            )
            is AddressBookRoute.EditContact -> editContactComponentFactory.create(
                context = childByContext(componentContext),
                params = EditContactComponent.Params(
                    contactId = config.contactId?.let(::ContactId),
                    onBackClick = { navigation.pop() },
                ),
            )
        }

    @AssistedFactory
    interface Factory : AddressBookComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddressBookComponent.Params,
        ): DefaultAddressBookComponent
    }
}