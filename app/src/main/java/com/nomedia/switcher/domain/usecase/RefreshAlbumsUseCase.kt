package com.nomedia.switcher.domain.usecase

import com.nomedia.switcher.data.local.album.AlbumRecordEntity
import com.nomedia.switcher.data.local.settings.AlbumSortMode
import com.nomedia.switcher.data.media.AlbumCandidate
import com.nomedia.switcher.domain.model.AlbumEntry

class RefreshAlbumsUseCase(
    private val observeAlbumsUseCase: ObserveAlbumsUseCase = ObserveAlbumsUseCase(),
) {
    operator fun invoke(
        records: List<AlbumRecordEntity>,
        scan: List<AlbumCandidate>,
        pinHidden: Boolean,
        sortMode: AlbumSortMode = AlbumSortMode.ByName,
        scanCompleted: Boolean = false,
    ): List<AlbumEntry> {
        return observeAlbumsUseCase.merge(
            records = records,
            scan = scan,
            pinHidden = pinHidden,
            sortMode = sortMode,
            scanCompleted = scanCompleted,
        )
    }
}
