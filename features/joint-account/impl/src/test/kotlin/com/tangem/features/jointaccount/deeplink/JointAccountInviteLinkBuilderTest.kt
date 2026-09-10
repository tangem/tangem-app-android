package com.tangem.features.jointaccount.deeplink

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/** The invite id is a path segment: reserved characters from the backend must not change the link's meaning. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountInviteLinkBuilderTest {

    @ParameterizedTest
    @ProvideTestModels
    fun build(model: LinkModel) {
        // Act
        val actual = JointAccountInviteLinkBuilder.build(inviteId = model.inviteId)

        // Assert
        assertThat(actual).isEqualTo("https://tangem.com/joint-account/join/${model.expectedSegment}")
    }

    data class LinkModel(val inviteId: String, val expectedSegment: String)

    private fun provideTestModels() = listOf(
        LinkModel(inviteId = "00AB12FF", expectedSegment = "00AB12FF"),
        LinkModel(inviteId = "a+b", expectedSegment = "a%2Bb"),
        LinkModel(inviteId = "a/b", expectedSegment = "a%2Fb"),
        LinkModel(inviteId = "a b", expectedSegment = "a%20b"),
        LinkModel(inviteId = "a?b#c", expectedSegment = "a%3Fb%23c"),
        LinkModel(inviteId = "a%b", expectedSegment = "a%25b"),
    )
}