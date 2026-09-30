package com.tangem.features.storiesv2.impl.content.remote

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoriesV2Response
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2ActionDTO
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2SlideDTO
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2SourceDTO
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.features.storiesv2.impl.content.StoryV2Action
import com.tangem.features.storiesv2.impl.content.StoryV2ContentMode
import com.tangem.features.storiesv2.impl.content.StoryV2EndBehavior
import com.tangem.features.storiesv2.impl.content.StoryV2Haptic
import com.tangem.features.storiesv2.impl.content.remote.RemoteStoryV2ConversionResult.Converted
import com.tangem.features.storiesv2.impl.content.remote.RemoteStoryV2ConversionResult.Issue
import com.tangem.features.storiesv2.impl.content.remote.RemoteStoryV2ConversionResult.Rejected
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class StoriesV2ResponseConverterTest {

    @Test
    fun `GIVEN the dev response WHEN convert THEN one story with both placements and clustered haptics is converted`() {
        // Arrange
        val response = StoryV2Json.decode(StoryV2Json.resource(path = DEV_RESPONSE_PATH))
        val expected = Converted(
            stories = listOf(
                RemoteStoryV2(
                    slug = "hardware_upgrade",
                    placements = setOf("main_upgrade", "wallet_settings_upgrade"),
                    displayPolicy = RemoteStoryV2.DisplayPolicy.ALWAYS,
                    endBehavior = StoryV2EndBehavior.LOOP,
                    actions = listOf(
                        storyAction(
                            id = "hardware_upgrade_action_1",
                            label = "Get Tangem",
                            style = StoryV2Action.Style.PRIMARY,
                            target = StoryV2Action.Target.Deeplink(uri = "tangem://hardware_wallet"),
                        ),
                    ),
                    slides = listOf(
                        remoteSlide(
                            id = "2",
                            assetType = RemoteStoryV2Slide.AssetType.VIDEO,
                            contentMode = StoryV2ContentMode.CONTAIN,
                            durationMs = 10_000L,
                            title = "One tap to control crypto",
                            subtitle = "Confirm every action with your Tangem",
                            haptics = listOf(
                                tick(atMs = 600L),
                                tick(atMs = 1_050L),
                                tick(atMs = 1_450L, pulses = 2, pulseIntervalMs = 110L),
                            ),
                            source = devFile(
                                fileName = "2-noaudio-1789394130917-01864701f474.mp4",
                                sizeBytes = 2_513_699L,
                            ),
                        ),
                        remoteSlide(
                            id = "3",
                            assetType = RemoteStoryV2Slide.AssetType.VIDEO,
                            contentMode = StoryV2ContentMode.CONTAIN,
                            durationMs = 10_000L,
                            title = "One wallet, multiple cards",
                            subtitle = "Use any of your cards to access your wallet",
                            haptics = listOf(tick(atMs = 4_700L)),
                            source = devFile(
                                fileName = "1-noaudio-1789394174286-5d99a8c44f75.mp4",
                                sizeBytes = 2_514_384L,
                            ),
                        ),
                        remoteSlide(
                            id = "4",
                            assetType = RemoteStoryV2Slide.AssetType.VIDEO,
                            contentMode = StoryV2ContentMode.CONTAIN,
                            durationMs = 10_000L,
                            title = "Your keys, your crypto",
                            subtitle = "Your private keys never leave the card.",
                            haptics = listOf(tick(atMs = 350L, pulses = 12, pulseIntervalMs = 30L)),
                            source = devFile(
                                fileName = "3-noaudio-1789394196233-fca620c349a8.mp4",
                                sizeBytes = 2_500_683L,
                            ),
                        ),
                        remoteSlide(
                            id = "5",
                            title = "Trusted by millions",
                            subtitle = "Join the self-custody wallet used worldwide",
                            assetType = RemoteStoryV2Slide.AssetType.VIDEO,
                            contentMode = StoryV2ContentMode.CONTAIN,
                            durationMs = 10_017L,
                            haptics = listOf(tick(atMs = 250L, pulses = 8, pulseIntervalMs = 30L), tick(atMs = 1_500L)),
                            source = devFile(
                                fileName = "4-noaudio-1789394212445-92fdc0867305.mp4",
                                sizeBytes = 2_506_594L,
                            ),
                        ),
                    ),
                ),
            ),
            issues = emptyList(),
        )

        // Act
        val actual = StoriesV2ResponseConverter(requestedSlugs = setOf("hardware_upgrade")).convert(response)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Nested
    inner class Response {

        @Test
        fun `GIVEN an empty response WHEN convert THEN nothing is converted and nothing is reported`() {
            // Act
            val actual = convert(response = emptyMap())

            // Assert
            assertThat(actual).isEqualTo(Converted(stories = emptyList(), issues = emptyList()))
        }

        @Test
        fun `GIVEN an unrequested slug under a known placement WHEN convert THEN the whole response is rejected`() {
            // Arrange
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(), storyDto(slug = "other")))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(Rejected(StoryV2ErrorReason.UNKNOWN_SCHEMA))
        }

        @Test
        fun `GIVEN a broken unrequested story under an unknown placement WHEN convert THEN the response is rejected`() {
            // Arrange
            val response = mapOf(
                MAIN_PLACEMENT to listOf(storyDto()),
                "unknown_placement" to listOf(storyDto(slug = "other", displayPolicy = null)),
            )

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(Rejected(StoryV2ErrorReason.UNKNOWN_SCHEMA))
        }

        @Test
        fun `GIVEN story entries without a slug WHEN convert THEN each is reported and the valid story survives`() {
            // Arrange
            val response = mapOf(
                MAIN_PLACEMENT to listOf(
                    storyDto(slug = null),
                    storyDto(slug = null, displayPolicy = "once"),
                    storyDto(),
                ),
            )

            // Act
            val actual = convert(response)

            // Assert
            val schemaIssue = Issue(storyId = null, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_SCHEMA)
            assertThat(actual).isEqualTo(
                Converted(stories = listOf(remoteStory()), issues = listOf(schemaIssue, schemaIssue)),
            )
        }

        @Test
        fun `GIVEN a story with a blank slug WHEN convert THEN only it is reported and the response is kept`() {
            // Arrange
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(slug = " "), storyDto()))

            // Act
            val actual = convert(response)

            // Assert
            val schemaIssue = Issue(storyId = null, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_SCHEMA)
            assertThat(actual).isEqualTo(Converted(stories = listOf(remoteStory()), issues = listOf(schemaIssue)))
        }

        @Test
        fun `GIVEN a placement without a story list WHEN convert THEN it is ignored`() {
            // Arrange
            val response = mapOf(MAIN_PLACEMENT to null, SETTINGS_PLACEMENT to listOf(storyDto()))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(stories = listOf(remoteStory(placements = setOf(SETTINGS_PLACEMENT))), issues = emptyList()),
            )
        }
    }

    @Nested
    inner class Placements {

        @Test
        fun `GIVEN one story under both known placements WHEN convert THEN it is one story with both placements`() {
            // Arrange
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto()), SETTINGS_PLACEMENT to listOf(storyDto()))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = listOf(remoteStory(placements = setOf(MAIN_PLACEMENT, SETTINGS_PLACEMENT))),
                    issues = emptyList(),
                ),
            )
        }

        @Test
        fun `GIVEN a story only under an unknown placement WHEN convert THEN it is excluded and reported`() {
            // Arrange
            val response = mapOf("unknown_placement" to listOf(storyDto()))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = emptyList(),
                    issues = listOf(
                        Issue(storyId = SLUG, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_PLACEMENT),
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN a story under a known and an unknown placement WHEN convert THEN only the known one is kept`() {
            // Arrange
            val response = mapOf("unknown_placement" to listOf(storyDto()), MAIN_PLACEMENT to listOf(storyDto()))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(stories = listOf(remoteStory(placements = setOf(MAIN_PLACEMENT))), issues = emptyList()),
            )
        }

        @Test
        fun `GIVEN a broken first copy and a valid second copy WHEN convert THEN the first copy decides`() {
            // Arrange
            val response = mapOf(
                MAIN_PLACEMENT to listOf(storyDto(displayPolicy = null)),
                SETTINGS_PLACEMENT to listOf(storyDto()),
            )

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = emptyList(),
                    issues = listOf(Issue(storyId = SLUG, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_POLICY)),
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Policies {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an unknown or missing policy WHEN convert THEN the story is excluded as unknown policy`(
            model: PolicyModel,
        ) {
            // Arrange
            val response = mapOf(
                MAIN_PLACEMENT to listOf(
                    storyDto(displayPolicy = model.displayPolicy, endBehaviour = model.endBehaviour),
                ),
            )

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = emptyList(),
                    issues = listOf(Issue(storyId = SLUG, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_POLICY)),
                ),
            )
        }

        @Test
        fun `GIVEN once and finish WHEN convert THEN they map to the story policies`() {
            // Arrange
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(displayPolicy = "once", endBehaviour = "finish")))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = listOf(
                        remoteStory(
                            displayPolicy = RemoteStoryV2.DisplayPolicy.ONCE,
                            endBehavior = StoryV2EndBehavior.FINISH,
                        ),
                    ),
                    issues = emptyList(),
                ),
            )
        }

        private fun provideTestModels() = listOf(
            PolicyModel(displayPolicy = null, endBehaviour = "loop"),
            PolicyModel(displayPolicy = "sometimes", endBehaviour = "loop"),
            PolicyModel(displayPolicy = "ALWAYS", endBehaviour = "loop"),
            PolicyModel(displayPolicy = "always", endBehaviour = null),
            PolicyModel(displayPolicy = "always", endBehaviour = "stop"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SlideLists {

        @Test
        fun `GIVEN distinct slide ids WHEN convert THEN every slide keeps its id`() {
            // Arrange
            val slides = listOf(slideDto(id = "2"), slideDto(id = "intro"))
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(slides = slides)))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = listOf(remoteStory(slides = listOf(remoteSlide(id = "2"), remoteSlide(id = "intro")))),
                    issues = emptyList(),
                ),
            )
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN missing, empty or repeated slides or slide ids WHEN convert THEN the story is excluded`(
            model: SlideListModel,
        ) {
            // Arrange
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(slides = model.slides)))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = emptyList(),
                    issues = listOf(Issue(storyId = SLUG, slideId = null, reason = model.reason)),
                ),
            )
        }

        private fun provideTestModels() = listOf(
            SlideListModel(slides = listOf(slideDto(id = null), slideDto(id = "2"))),
            SlideListModel(slides = listOf(slideDto(id = ""), slideDto(id = "2"))),
            SlideListModel(slides = listOf(slideDto(id = " "), slideDto(id = "2"))),
            SlideListModel(slides = listOf(slideDto(id = "2"), slideDto(id = "2"))),
            SlideListModel(slides = null),
            SlideListModel(slides = emptyList(), reason = StoryV2ErrorReason.UNKNOWN_ASSET),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Slides {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a broken slide WHEN convert THEN only that slide is dropped and reported`(model: BrokenSlideModel) {
            // Arrange
            val slides = listOf(model.slide, slideDto(id = "2"))
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(slides = slides)))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = listOf(remoteStory(slides = listOf(remoteSlide(id = "2")))),
                    issues = listOf(Issue(storyId = SLUG, slideId = "1", reason = model.reason)),
                ),
            )
        }

        @Test
        fun `GIVEN every slide broken WHEN convert THEN the story is excluded`() {
            // Arrange
            val slides = listOf(slideDto(id = "1", assetType = "gif"), slideDto(id = "2", contentMode = null))
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(slides = slides)))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = emptyList(),
                    issues = listOf(
                        Issue(storyId = SLUG, slideId = "1", reason = StoryV2ErrorReason.UNKNOWN_ASSET),
                        Issue(storyId = SLUG, slideId = "2", reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
                        Issue(storyId = SLUG, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_ASSET),
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN every asset type and content mode WHEN convert THEN they map one to one`() {
            // Arrange
            val slides = listOf(
                slideDto(id = "1", assetType = "video", contentMode = "cover", subtitle = ""),
                slideDto(id = "2", assetType = "lottie", contentMode = "contain"),
                slideDto(id = "3", assetType = "image", contentMode = "cover"),
            )
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(slides = slides)))

            // Act
            val actual = convert(response)

            // Assert
            val expectedSlides = listOf(
                remoteSlide(
                    id = "1",
                    subtitle = "",
                    assetType = RemoteStoryV2Slide.AssetType.VIDEO,
                    contentMode = StoryV2ContentMode.COVER,
                ),
                remoteSlide(
                    id = "2",
                    assetType = RemoteStoryV2Slide.AssetType.LOTTIE,
                    contentMode = StoryV2ContentMode.CONTAIN,
                ),
                remoteSlide(
                    id = "3",
                    assetType = RemoteStoryV2Slide.AssetType.IMAGE,
                    contentMode = StoryV2ContentMode.COVER,
                ),
            )
            assertThat(actual).isEqualTo(
                Converted(stories = listOf(remoteStory(slides = expectedSlides)), issues = emptyList()),
            )
        }

        private fun provideTestModels() = listOf(
            BrokenSlideModel(slide = slideDto(assetType = "gif"), reason = StoryV2ErrorReason.UNKNOWN_ASSET),
            BrokenSlideModel(slide = slideDto(assetType = null), reason = StoryV2ErrorReason.UNKNOWN_ASSET),
            BrokenSlideModel(slide = slideDto(contentMode = null), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            BrokenSlideModel(slide = slideDto(contentMode = "fill"), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            BrokenSlideModel(slide = slideDto(durationMs = 0), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            BrokenSlideModel(slide = slideDto(durationMs = -1_000), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            BrokenSlideModel(slide = slideDto(durationMs = null), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            BrokenSlideModel(slide = slideDto(title = null), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            BrokenSlideModel(slide = slideDto(subtitle = null), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            BrokenSlideModel(slide = slideDto(source = null), reason = StoryV2ErrorReason.NO_COMPATIBLE_SOURCE),
            brokenSource(sourceDto(url = null)),
            brokenSource(sourceDto(mimetype = null)),
            brokenSource(sourceDto(mimetype = " ")),
            brokenSource(sourceDto(sizeBytes = null)),
            brokenSource(sourceDto(sizeBytes = 0)),
            brokenSource(sourceDto(sizeBytes = -1)),
            brokenSource(sourceDto(url = "http://cdn.tangem.com/stories/slide_1.mp4")),
            brokenSource(sourceDto(url = "https://cdn.tangem.com/stories/slide_1.mp4?q")),
            brokenSource(sourceDto(url = "https://cdn.tangem.com/stories/slide_1.mp4#f")),
            brokenSource(sourceDto(url = "https://u@h/p")),
            brokenSource(sourceDto(url = "https://cdn.tangem.com")),
            brokenSource(sourceDto(url = "https://cdn.tangem.com/stories/slide 1.mp4")),
        )

        private fun brokenSource(source: StoryV2SourceDTO) = BrokenSlideModel(
            slide = slideDto(source = source),
            reason = StoryV2ErrorReason.NO_COMPATIBLE_SOURCE,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Haptics {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN haptic marks WHEN convert THEN unreachable marks are dropped and the rest clustered`(
            model: HapticsModel,
        ) {
            // Arrange
            val slide = slideDto(assetType = model.assetType, hapticAtMs = model.hapticAtMs)
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(slides = listOf(slide))))

            // Act
            val actual = convert(response)

            // Assert
            val expectedSlide = remoteSlide(assetType = model.expectedAssetType, haptics = model.expected)
            assertThat(actual).isEqualTo(
                Converted(stories = listOf(remoteStory(slides = listOf(expectedSlide))), issues = emptyList()),
            )
        }

        private fun provideTestModels() = listOf(
            HapticsModel(hapticAtMs = listOf(-10, 100, SLIDE_DURATION_MS, 6_000), expected = listOf(tick(100L))),
            HapticsModel(hapticAtMs = listOf(0), expected = listOf(tick(0L))),
            HapticsModel(hapticAtMs = listOf(SLIDE_DURATION_MS - 1), expected = listOf(tick(SLIDE_DURATION_MS - 1))),
            HapticsModel(
                hapticAtMs = listOf(100, 130, 160, 1_000),
                expected = listOf(tick(atMs = 100L, pulses = 3, pulseIntervalMs = 30L), tick(atMs = 1_000L)),
            ),
            HapticsModel(
                assetType = "lottie",
                hapticAtMs = listOf(100, 130),
                expected = listOf(tick(atMs = 100L, pulses = 2, pulseIntervalMs = 30L)),
            ),
            HapticsModel(assetType = "image", hapticAtMs = listOf(100), expected = emptyList()),
            HapticsModel(hapticAtMs = emptyList(), expected = emptyList()),
            HapticsModel(hapticAtMs = null, expected = emptyList()),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AllowedTargets {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an allowed target WHEN convert THEN the action points at it`(model: AllowedTargetModel) {
            // Arrange
            val action = actionDto(deeplink = model.deeplink)
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(actions = listOf(action))))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = listOf(remoteStory(actions = listOf(storyAction(target = model.expected)))),
                    issues = emptyList(),
                ),
            )
        }

        private fun provideTestModels(): List<AllowedTargetModel> {
            val deeplinks = listOf(HARDWARE_WALLET_DEEPLINK, "tangem://hardware_wallet/buy?ref=story").map { uri ->
                AllowedTargetModel(deeplink = uri, expected = StoryV2Action.Target.Deeplink(uri = uri))
            }
            val webUrls = listOf(
                "https://promo.tangem.com/upgrade",
                "https://tangem.com",
                "https://www.tangem.com/en/?utm=story",
                "https://buy.tangem.com/products",
                "https://app.tangem.com/wallet",
                "https://tangem.surveysparrow.com/s/story",
                "https://feedback.tangem.com#form",
            ).map { url -> AllowedTargetModel(deeplink = url, expected = StoryV2Action.Target.Web(url = url)) }

            return deeplinks + webUrls
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HiddenActions {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a broken action WHEN convert THEN only it is hidden and the neighbour keeps its raw index id`(
            model: HiddenActionModel,
        ) {
            // Arrange
            val neighbour = actionDto(label = "Later", style = "secondary", deeplink = "https://tangem.com")
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(actions = listOf(model.action, neighbour))))

            // Act
            val actual = convert(response)

            // Assert
            val expectedNeighbour = storyAction(
                id = "${SLUG}_action_2",
                label = "Later",
                style = StoryV2Action.Style.SECONDARY,
                target = StoryV2Action.Target.Web(url = "https://tangem.com"),
            )
            assertThat(actual).isEqualTo(
                Converted(
                    stories = listOf(remoteStory(actions = listOf(expectedNeighbour))),
                    issues = listOf(Issue(storyId = SLUG, slideId = null, reason = model.reason)),
                ),
            )
        }

        private fun provideTestModels() = listOf(
            hiddenTarget(deeplink = "tangem://unknown", reason = StoryV2ErrorReason.UNKNOWN_TARGET),
            hiddenTarget(deeplink = "tangem://", reason = StoryV2ErrorReason.UNKNOWN_TARGET),
            hiddenTarget(deeplink = null, reason = StoryV2ErrorReason.UNKNOWN_TARGET),
            hiddenTarget(deeplink = "http://tangem.com", reason = StoryV2ErrorReason.UNKNOWN_TARGET),
            hiddenTarget(deeplink = "javascript:alert(1)", reason = StoryV2ErrorReason.UNKNOWN_TARGET),
            hiddenTarget(deeplink = "https://evil.com", reason = StoryV2ErrorReason.FORBIDDEN_TARGET),
            hiddenTarget(deeplink = "https://tangem.com.evil.com", reason = StoryV2ErrorReason.FORBIDDEN_TARGET),
            hiddenTarget(deeplink = "https://[REDACTED_EMAIL]", reason = StoryV2ErrorReason.FORBIDDEN_TARGET),
            hiddenTarget(deeplink = "https://tangem.com:8443", reason = StoryV2ErrorReason.FORBIDDEN_TARGET),
            HiddenActionModel(action = actionDto(label = null), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            HiddenActionModel(action = actionDto(label = " "), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            HiddenActionModel(action = actionDto(style = "tertiary"), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            HiddenActionModel(action = actionDto(style = null), reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
        )

        private fun hiddenTarget(deeplink: String?, reason: StoryV2ErrorReason) = HiddenActionModel(
            action = actionDto(deeplink = deeplink),
            reason = reason,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ActionSets {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN more than two actions or a repeated style WHEN convert THEN the whole set is hidden`(
            model: ActionSetModel,
        ) {
            // Arrange
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(actions = model.actions)))

            // Act
            val actual = convert(response)

            // Assert
            assertThat(actual).isEqualTo(
                Converted(
                    stories = listOf(remoteStory(actions = emptyList())),
                    issues = listOf(Issue(storyId = SLUG, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_SCHEMA)),
                ),
            )
        }

        @Test
        fun `GIVEN actions with their own ids WHEN convert THEN the ids are kept`() {
            // Arrange
            val actions = listOf(
                actionDto(id = "buy_now"),
                actionDto(id = "7", label = "Later", style = "secondary", deeplink = "https://tangem.com"),
            )
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(actions = actions)))

            // Act
            val actual = convert(response)

            // Assert
            val expectedActions = listOf(
                storyAction(id = "buy_now"),
                laterAction(id = "7"),
            )
            assertThat(actual).isEqualTo(
                Converted(stories = listOf(remoteStory(actions = expectedActions)), issues = emptyList()),
            )
        }

        @Test
        fun `GIVEN actions without a usable id WHEN convert THEN each gets the slug and position fallback id`() {
            // Arrange
            val actions = listOf(
                actionDto(id = null),
                actionDto(id = " ", label = "Later", style = "secondary", deeplink = "https://tangem.com"),
            )
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(actions = actions)))

            // Act
            val actual = convert(response)

            // Assert
            val expectedActions = listOf(
                storyAction(id = "${SLUG}_action_1"),
                laterAction(id = "${SLUG}_action_2"),
            )
            assertThat(actual).isEqualTo(
                Converted(stories = listOf(remoteStory(actions = expectedActions)), issues = emptyList()),
            )
        }

        private fun laterAction(id: String) = storyAction(
            id = id,
            label = "Later",
            style = StoryV2Action.Style.SECONDARY,
            target = StoryV2Action.Target.Web(url = "https://tangem.com"),
        )

        private fun provideTestModels() = listOf(
            ActionSetModel(
                actions = listOf(
                    actionDto(style = "primary"),
                    actionDto(style = "secondary"),
                    actionDto(style = "tertiary"),
                ),
            ),
            ActionSetModel(actions = listOf(actionDto(style = "primary"), actionDto(style = "primary"))),
        )
    }

    /** Moshi does not enforce the nullability of type arguments, so a `null` element can arrive in any list. */
    @Nested
    inner class NullElements {

        @Test
        fun `GIVEN a null story entry WHEN convert THEN it is reported and the valid story survives`() {
            // Arrange
            val response = mapOf(MAIN_PLACEMENT to listOf(null, storyDto()))

            // Act
            val actual = convert(response)

            // Assert
            val schemaIssue = Issue(storyId = null, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_SCHEMA)
            assertThat(actual).isEqualTo(Converted(stories = listOf(remoteStory()), issues = listOf(schemaIssue)))
        }

        @Test
        fun `GIVEN a null slide WHEN convert THEN the story is excluded as a slide without an id`() {
            // Arrange
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(slides = listOf(slideDto(), null))))

            // Act
            val actual = convert(response)

            // Assert
            val issue = Issue(storyId = SLUG, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_SCHEMA)
            assertThat(actual).isEqualTo(Converted(stories = emptyList(), issues = listOf(issue)))
        }

        @Test
        fun `GIVEN a null haptic mark WHEN convert THEN only the mark is dropped`() {
            // Arrange
            val slide = slideDto(hapticAtMs = listOf(100L, null, 400L))
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(slides = listOf(slide))))

            // Act
            val actual = convert(response)

            // Assert
            val expectedSlide = remoteSlide(haptics = StoryV2HapticClusterizer.cluster(listOf(100L, 400L)))
            assertThat(actual).isEqualTo(
                Converted(stories = listOf(remoteStory(slides = listOf(expectedSlide))), issues = emptyList()),
            )
        }

        @Test
        fun `GIVEN a null action WHEN convert THEN only it is hidden and the neighbour keeps its raw index id`() {
            // Arrange
            val response = mapOf(MAIN_PLACEMENT to listOf(storyDto(actions = listOf(null, actionDto()))))

            // Act
            val actual = convert(response)

            // Assert
            val expectedAction = StoryV2Action(
                id = "${SLUG}_action_2",
                label = stringReference("Get Tangem"),
                style = StoryV2Action.Style.PRIMARY,
                target = StoryV2Action.Target.Deeplink(uri = HARDWARE_WALLET_DEEPLINK),
            )
            val issue = Issue(storyId = SLUG, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_SCHEMA)
            assertThat(actual).isEqualTo(
                Converted(stories = listOf(remoteStory(actions = listOf(expectedAction))), issues = listOf(issue)),
            )
        }
    }

    private fun convert(response: StoriesV2Response): RemoteStoryV2ConversionResult {
        return StoriesV2ResponseConverter(requestedSlugs = setOf(SLUG)).convert(response)
    }

    private fun devFile(fileName: String, sizeBytes: Long) = RemoteStoryV2File(
        url = "https://s3.eu-central-1.amazonaws.com/tangem.api.dev/stories/assets/$fileName",
        sizeBytes = sizeBytes,
    )

    internal data class PolicyModel(val displayPolicy: String?, val endBehaviour: String?)

    internal data class SlideListModel(
        val slides: List<StoryV2SlideDTO>?,
        val reason: StoryV2ErrorReason = StoryV2ErrorReason.UNKNOWN_SCHEMA,
    )

    internal data class BrokenSlideModel(val slide: StoryV2SlideDTO, val reason: StoryV2ErrorReason)

    internal data class HapticsModel(
        val hapticAtMs: List<Long>?,
        val expected: List<StoryV2Haptic>,
        val assetType: String = "video",
    ) {
        val expectedAssetType: RemoteStoryV2Slide.AssetType
            get() = RemoteStoryV2Slide.AssetType.valueOf(assetType.uppercase())
    }

    internal data class AllowedTargetModel(val deeplink: String, val expected: StoryV2Action.Target)

    internal data class HiddenActionModel(val action: StoryV2ActionDTO, val reason: StoryV2ErrorReason)

    internal data class ActionSetModel(val actions: List<StoryV2ActionDTO>)
}