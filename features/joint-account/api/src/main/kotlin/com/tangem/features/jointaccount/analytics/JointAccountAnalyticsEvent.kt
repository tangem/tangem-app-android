package com.tangem.features.jointaccount.analytics

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.remote.response.ApiResponseError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class JointAccountAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = CATEGORY, event = event, params = params) {

    class ButtonCreateJointAccount(source: AppRoute.JointAccountCreation.Source) : JointAccountAnalyticsEvent(
        event = "Button - Create Joint Account",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )

    class IntroScreenOpened(source: AppRoute.JointAccountCreation.Source) : JointAccountAnalyticsEvent(
        event = "Intro Screen Opened",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )

    class JointAccountCreated(membersCount: Int, threshold: Int) : JointAccountAnalyticsEvent(
        event = "Joint Account Created",
        params = mapOf(
            MEMBERS_COUNT to membersCount.toString(),
            THRESHOLD to threshold.toString(),
        ),
    )

    class NoticeAccountsLimitReached(source: LimitSource) : JointAccountAnalyticsEvent(
        event = "Notice - Accounts Limit Reached",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )

    /**
     * An error the user was shown. [errorCode] is the backend's own `code` from the error body when there is one,
     * else the HTTP status; absent for a failure that never reached the backend.
     */
    class JointAccountError private constructor(
        source: ErrorSource,
        errorCode: String?,
        errorMessage: String,
    ) : JointAccountAnalyticsEvent(
        event = "Joint Account Error",
        params = buildMap {
            put(AnalyticsParam.SOURCE, source.value)
            errorCode?.let { put(AnalyticsParam.ERROR_CODE, it) }
            put(AnalyticsParam.ERROR_MESSAGE, errorMessage)
        },
    ) {

        constructor(source: ErrorSource, cause: Throwable) : this(
            source = source,
            errorCode = (cause as? ApiResponseError.HttpException)?.errorCode(),
            errorMessage = cause.message ?: cause::class.java.simpleName,
        )

        /** A failure detected in the app, with no backend response behind it */
        constructor(source: ErrorSource, errorMessage: String) : this(
            source = source,
            errorCode = null,
            errorMessage = errorMessage,
        )
    }

    enum class LimitSource(val value: String) {
        Create("Create"),
        Join("Join"),
    }

    enum class ErrorSource(val value: String) {
        Create("Create"),
        Join("Join"),
        CreateOperation("Create Operation"),
        Sign("Sign"),
        Decline("Decline"),
        Delete("Delete"),
    }

    private companion object {
        const val CATEGORY = "Joint Account"
        const val MEMBERS_COUNT = "Members Count"
        const val THRESHOLD = "Threshold"
    }
}

private val AppRoute.JointAccountCreation.Source.value: String
    get() = when (this) {
        AppRoute.JointAccountCreation.Source.AddAccount -> "Add Account"
        AppRoute.JointAccountCreation.Source.MainScreen -> "Main Screen"
    }

private fun ApiResponseError.HttpException.errorCode(): String = bodyCode() ?: code.numericCode.toString()

private fun ApiResponseError.HttpException.bodyCode(): String? = errorBody?.let { body ->
    runCatching { Json.parseToJsonElement(body).jsonObject["code"]?.jsonPrimitive?.contentOrNull }.getOrNull()
}