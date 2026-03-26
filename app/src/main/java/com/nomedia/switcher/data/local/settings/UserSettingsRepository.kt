package com.nomedia.switcher.data.local.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow

private val Context.userSettingsDataStore: DataStore<UserSettings> by dataStore(
    fileName = "user-settings.preferences",
    serializer = UserSettingsSerializer,
)

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
        fun create(context: Context): UserSettingsRepository {
            return UserSettingsRepository(
                dataStore = context.applicationContext.userSettingsDataStore,
            )
        }
    }
}
