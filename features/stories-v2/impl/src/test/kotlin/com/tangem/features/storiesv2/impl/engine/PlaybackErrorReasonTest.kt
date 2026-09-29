package com.tangem.features.storiesv2.impl.engine

import androidx.media3.common.PlaybackException
import com.google.common.truth.Truth.assertThat
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PlaybackErrorReasonTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a playback error code WHEN mapped THEN the matching error reason is returned`(model: ErrorCodeModel) {
        // Act
        val actual = playbackErrorReason(errorCode = model.errorCode)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ErrorCodeModel(
            errorCode = PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            expected = StoryV2ErrorReason.NOT_FOUND,
        ),
        ErrorCodeModel(
            errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED,
            expected = StoryV2ErrorReason.DECODE_FAILURE,
        ),
        ErrorCodeModel(
            errorCode = PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            expected = StoryV2ErrorReason.DECODE_FAILURE,
        ),
        ErrorCodeModel(
            errorCode = PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            expected = StoryV2ErrorReason.DECODE_FAILURE,
        ),
    )

    internal data class ErrorCodeModel(val errorCode: Int, val expected: StoryV2ErrorReason)
}