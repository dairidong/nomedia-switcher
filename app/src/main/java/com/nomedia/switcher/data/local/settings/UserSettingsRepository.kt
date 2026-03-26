package com.nomedia.switcher.data.local.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

class UserSettingsRepository(
    private val dataStore: DataStore<UserSettings>,
) {
    val settings: Flow<UserSettings> = dataStore.data

    suspend fun setPinHiddenAlbumsToTop(enabled: Boolean) {
        dataStore.updateData { current ->
            current.copy(pinHiddenAlbumsToTop = enabled)
        }
    }

    companion object {
        fun create(
            context: Context,
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ): UserSettingsRepository {
            return UserSettingsRepository(
                dataStore = DataStoreFactory.create(
                    serializer = UserSettingsSerializer,
                    scope = scope,
                    produceFile = { context.dataStoreFile("user-settings.preferences") },
                ),
            )
        }
    }
}
