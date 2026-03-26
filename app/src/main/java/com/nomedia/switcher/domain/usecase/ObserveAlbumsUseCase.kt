package com.nomedia.switcher.domain.usecase

import com.nomedia.switcher.data.AlbumRepositoryImpl
import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.media.AlbumCandidate
import com.nomedia.switcher.domain.model.AlbumEntry
import com.nomedia.switcher.domain.repository.AlbumRepository

class ObserveAlbumsUseCase(
    private val albumRepository: AlbumRepository = AlbumRepositoryImpl(),
) {
    fun merge(
        records: List<AlbumRecordEntity>,
        scan: List<AlbumCandidate>,
        pinHidden: Boolean,
        scanCompleted: Boolean = false,
    ): List<AlbumEntry> {
        return albumRepository.merge(records, scan, pinHidden, scanCompleted)
    }
}
