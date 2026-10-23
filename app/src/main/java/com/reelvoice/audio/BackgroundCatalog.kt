package com.reelvoice.audio

/** Server catalogue is authoritative. A removed selection must never become another sound. */
data class BackgroundCatalog(
    val items: List<VoiceBackground> = emptyList(),
    val selectedId: String? = null,
    val loading: Boolean = false,
    val error: String? = null
) {
    val selectedIndex: Int get() = items.indexOfFirst { it.id == selectedId }
    val selected: VoiceBackground? get() = items.getOrNull(selectedIndex)

    fun select(index: Int): BackgroundCatalog = items.getOrNull(index)?.let {
        copy(selectedId = it.id)
    } ?: this

    fun loaded(backgrounds: List<VoiceBackground>): BackgroundCatalog {
        val valid = backgrounds.filter { it.id.isNotBlank() }.distinctBy { it.id }
        return copy(items = valid, selectedId = selectedId ?: valid.firstOrNull()?.id,
            loading = false, error = null)
    }

    fun failed(): BackgroundCatalog = copy(items = emptyList(), loading = false,
        error = "Couldn't load sounds. Check your connection and try again.")
}
