package com.reelvoice.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecordingPresetTest {
    @Test fun recordingOffersOnlyStrongAndStudio() {
        assertEquals(listOf(CleaningPreset.STRONG, CleaningPreset.STUDIO), CleaningPreset.recordingPresets)
        assertFalse(CleaningPreset.recordingPresets.contains(CleaningPreset.NORMAL))
    }

    @Test fun legacyNormalCannotReachMicrophoneProcessing() {
        assertEquals(CleaningPreset.STRONG, CleaningPreset.NORMAL.forRecording())
        assertEquals(CleaningPreset.STRONG, CleaningPreset.STRONG.forRecording())
        assertEquals(CleaningPreset.STUDIO, CleaningPreset.STUDIO.forRecording())
    }

    @Test fun uploadedFilePresetContractIsUnchanged() {
        assertEquals(CleaningPreset.NORMAL, CleaningPreset.fromIndex(0))
        assertEquals("Normal", CleaningPreset.NORMAL.label)
        assertEquals(0.5f, CleaningPreset.NORMAL.inputLinearScale)
    }
}
