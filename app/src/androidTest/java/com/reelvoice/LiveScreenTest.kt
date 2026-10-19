package com.reelvoice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.reelvoice.audio.CleaningPreset
import com.reelvoice.ui.screens.AudioPair
import com.reelvoice.ui.screens.LiveScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LiveScreenTest {
    @get:Rule val compose = createComposeRule()

    private val audioPair = AudioPair("20261019_112500", "11:25 AM", "original_test.wav", "cleaned_test.wav", true)

    @Test fun onlyStrongAndStudioAreOfferedAndSelectionUsesTheRightPreset() {
        val preset = mutableStateOf(CleaningPreset.STRONG)
        compose.setContent {
            MaterialTheme {
                LiveScreen(cleaningPreset = preset.value, onCleaningPresetChange = { preset.value = it })
            }
        }
        compose.onNodeWithText("Normal").assertDoesNotExist()
        compose.onNodeWithText("Strong").assertIsSelected()
        compose.onNodeWithText("Studio").performClick()
        compose.onNodeWithText("Studio").assertIsSelected()
        compose.runOnIdle { assertEquals(CleaningPreset.STUDIO, preset.value) }
        compose.onNodeWithText("Strong").performClick()
        compose.runOnIdle { assertEquals(CleaningPreset.STRONG, preset.value) }
    }

    @Test fun recordingLocksSetupAndStopSaves() {
        val recording = mutableStateOf(false)
        var playbackStops = 0
        compose.setContent {
            MaterialTheme {
                LiveScreen(
                    isRecording = recording.value,
                    onStartRecording = { recording.value = true },
                    onStopRecording = { recording.value = false },
                    onStopFile = { playbackStops++ }
                )
            }
        }
        compose.onNodeWithText("Start recording").performScrollTo().performClick()
        compose.onNodeWithText("Strong").assertIsNotEnabled()
        compose.onNodeWithText("Studio").assertIsNotEnabled()
        compose.onNodeWithText("Video").assertIsNotEnabled()
        compose.onNodeWithText("Stop & save").performScrollTo().performClick()
        compose.onNodeWithText("Strong").assertIsEnabled()
        compose.runOnIdle { assertEquals(1, playbackStops) }
    }

    @Test fun latestResultDoesNotPushSetupBelowTheFoldOrDuplicateTheHistoryList() {
        var playedFile: String? = null
        compose.setContent {
            MaterialTheme {
                LiveScreen(audioPairs = listOf(audioPair), onPlayFile = { playedFile = it })
            }
        }
        compose.onNodeWithText("Start recording").assertIsDisplayed()
        compose.onNodeWithText("Strong").assertIsDisplayed()
        compose.onNodeWithText("Cleaned").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(audioPair.cleanedFile, playedFile) }
        compose.onNodeWithText("Recent Recordings").assertDoesNotExist()
        compose.onAllNodesWithText("Last recording").assertCountEquals(1)
    }

    @Test fun rawVideoIsNotMisrepresentedAsACleanedRecording() {
        compose.setContent {
            MaterialTheme {
                LiveScreen(audioPairs = listOf(audioPair.copy(originalFile = "video_test.mp4", cleanedFile = "video_test.mp4")))
            }
        }
        compose.onNodeWithText("Play recording").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cleaned").assertDoesNotExist()
        compose.onNodeWithText("Original").assertDoesNotExist()
        compose.onNodeWithText("Feedback").assertDoesNotExist()
    }

    @Test fun videoOpensTheCameraInsteadOfStartingAnAudioTake() {
        val video = mutableStateOf(false)
        var cameraOpens = 0
        var audioStarts = 0
        compose.setContent {
            MaterialTheme {
                LiveScreen(
                    isVideoMode = video.value,
                    onToggleFormat = { video.value = it },
                    onStartVideoRecording = { cameraOpens++ },
                    onStartRecording = { audioStarts++ }
                )
            }
        }
        compose.onNodeWithText("Video").performClick()
        compose.onNodeWithText("Recording mode").assertDoesNotExist()
        compose.onNodeWithText("Open video camera").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, cameraOpens)
            assertEquals(0, audioStarts)
        }
        compose.onNodeWithText("Audio").performScrollTo().performClick()
        compose.onNodeWithText("Strong").assertIsSelected()
    }

    @Test fun recordAndResultRemainReachableWithLargeTextAndAShortViewport() {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.5f)
            ) {
                MaterialTheme {
                    Box(modifier = Modifier.height(500.dp)) {
                        LiveScreen(audioPairs = listOf(audioPair))
                    }
                }
            }
        }
        compose.onNodeWithText("Start recording").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cleaned").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Strong").performScrollTo().assertIsDisplayed()
    }

    @Test fun newlySavedTakeAppearsWhenTheActivityMutatesItsExistingStateList() {
        val pairs = androidx.compose.runtime.mutableStateListOf<AudioPair>()
        compose.setContent {
            MaterialTheme { LiveScreen(audioPairs = pairs) }
        }
        compose.onNodeWithText("Last recording").assertDoesNotExist()
        compose.runOnIdle { pairs.add(audioPair) }
        compose.onNodeWithText("Last recording").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cleaned").assertExists()
    }

    @Test fun uploadedClipsDoNotAppearAsTheLastRecording() {
        compose.setContent {
            MaterialTheme { LiveScreen(audioPairs = listOf(audioPair.copy(isRecording = false))) }
        }
        compose.onNodeWithText("Last recording").assertDoesNotExist()
    }
}
