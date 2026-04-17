package com.nomedia.switcher.domain.repository

import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.local.settings.AlbumSortMode
import com.nomedia.switcher.data.media.AlbumCandidate
import com.nomedia.switcher.domain.model.AlbumEntry

interface AlbumRepository {
    fun merge(
        records: List<AlbumRecordEntity>,
        scan: List<AlbumCandidate>,
        pinHidden: Boolean,
        sortMode: AlbumSortMode = AlbumSortMode.ByName,
        scanCompleted: Boolean = false,
    ): List<AlbumEntry>
}
