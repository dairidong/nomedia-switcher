package com.nomedia.switcher.data.local.settings

enum class AlbumSortMode {
    ByName,
    ByLatestMedia,
}

data class UserSettings(
    val pinHiddenAlbumsToTop: Boolean = true,
    val albumSortMode: AlbumSortMode = AlbumSortMode.ByName,
)
