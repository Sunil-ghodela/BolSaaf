package com.reelvoice.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Catalogue truth: no invented fallback item; selection survives reordering by ID. */
class BackgroundCatalogTest {
    private val rain = VoiceBackground("rain", "Rain", null, 0.15f)
    private val bhakti = VoiceBackground("bhakti", "Bhakti", "/previews/bhakti.mp3", 0.2f)
    private val cricket = VoiceBackground("cricket", "Cricket", "/previews/cricket.mp3", 0.25f)

    @Test fun selectionIsKeptByIdAfterRefreshReordersTheList() {
        val start = BackgroundCatalog().loaded(listOf(rain, bhakti, cricket)).select(2)
        assertEquals("cricket", start.selectedId)
        val reordered = start.loaded(listOf(cricket, rain, bhakti))
        assertEquals(0, reordered.selectedIndex)
        assertEquals("cricket", reordered.selectedId)
    }

    @Test fun emptyOrErrorCatalogueHasNoInventedFallbackSound() {
        val failed = BackgroundCatalog().failed()
        assertTrue(failed.items.isEmpty())
        assertTrue(failed.error != null)
        assertNull(failed.selectedId)
        assertNull(failed.selected)
    }

    @Test fun selectingAnUnavailableIndexKeepsThePreviousChoice() {
        val catalog = BackgroundCatalog().loaded(listOf(rain, bhakti)).select(1)
        val unchanged = catalog.select(5)
        assertEquals("bhakti", unchanged.selectedId)
    }

    @Test fun defaultVolumeFollowsTheSelectedSound() {
        val catalog = BackgroundCatalog().loaded(listOf(rain, cricket)).select(1)
        assertEquals(0.25f, catalog.selected?.defaultVolume)
        assertFalse(catalog.loading)
    }
}
