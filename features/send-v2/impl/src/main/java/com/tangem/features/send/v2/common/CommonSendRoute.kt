package com.tangem.features.send.v2.common

import com.tangem.features.send.v2.common.CommonSendRoute
import kotlinx.serialization.Serializable

<<<<<<<< HEAD:features/send-v2/impl/src/main/java/com/tangem/features/send/v2/common/CommonSendRoute.kt
internal sealed class CommonSendRoute : Route {

    abstract val isEditMode: Boolean

    @Serializable
    data object Empty : CommonSendRoute() {
========
@Serializable
internal sealed class SendRoute : CommonSendRoute {

    @Serializable
    data object Empty : SendRoute(), CommonSendRoute.Empty {
>>>>>>>> b110cd6df ([REDACTED_TASK_KEY] NFT Send. Made common routing abstraction):features/send-v2/impl/src/main/java/com/tangem/features/send/v2/send/SendRoute.kt
        override val isEditMode = false
    }

    @Serializable
<<<<<<<< HEAD:features/send-v2/impl/src/main/java/com/tangem/features/send/v2/common/CommonSendRoute.kt
    data object Confirm : CommonSendRoute() {
========
    data object Confirm : SendRoute(), CommonSendRoute.Confirm {
>>>>>>>> b110cd6df ([REDACTED_TASK_KEY] NFT Send. Made common routing abstraction):features/send-v2/impl/src/main/java/com/tangem/features/send/v2/send/SendRoute.kt
        override val isEditMode: Boolean = false
    }

    @Serializable
    data class Destination(
        override val isEditMode: Boolean,
<<<<<<<< HEAD:features/send-v2/impl/src/main/java/com/tangem/features/send/v2/common/CommonSendRoute.kt
    ) : CommonSendRoute()
========
    ) : SendRoute(), CommonSendRoute.Destination
>>>>>>>> b110cd6df ([REDACTED_TASK_KEY] NFT Send. Made common routing abstraction):features/send-v2/impl/src/main/java/com/tangem/features/send/v2/send/SendRoute.kt

    @Serializable
    data class Amount(
        override val isEditMode: Boolean,
<<<<<<<< HEAD:features/send-v2/impl/src/main/java/com/tangem/features/send/v2/common/CommonSendRoute.kt
    ) : CommonSendRoute()

    @Serializable
    data object Fee : CommonSendRoute() {
========
    ) : SendRoute(), CommonSendRoute.Amount

    @Serializable
    data object Fee : SendRoute(), CommonSendRoute.Fee {
>>>>>>>> b110cd6df ([REDACTED_TASK_KEY] NFT Send. Made common routing abstraction):features/send-v2/impl/src/main/java/com/tangem/features/send/v2/send/SendRoute.kt
        override val isEditMode: Boolean = true
    }
}