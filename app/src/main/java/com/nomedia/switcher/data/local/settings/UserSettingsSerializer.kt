package com.nomedia.switcher.data.local.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object UserSettingsSerializer : Serializer<UserSettings> {
    private const val PIN_HIDDEN_KEY = "pinHiddenAlbumsToTop"
    private const val SORT_MODE_KEY = "albumSortMode"

    override val defaultValue: UserSettings = UserSettings()

    override suspend fun readFrom(input: InputStream): UserSettings {
        val payload = input.readBytes().toString(StandardCharsets.UTF_8).trim()
        if (payload.isEmpty()) {
            return defaultValue
        }

        val keyValues = payload
            .lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val separatorIndex = line.indexOf('=')
                if (separatorIndex <= 0) {
                    throw CorruptionException("Cannot read user settings")
                }
                val key = line.substring(0, separatorIndex).trim()
                val value = line.substring(separatorIndex + 1).trim()
                key to value
            }

        return UserSettings(
            pinHiddenAlbumsToTop = keyValues[PIN_HIDDEN_KEY]?.let(::parseBooleanSetting)
                ?: defaultValue.pinHiddenAlbumsToTop,
            albumSortMode = keyValues[SORT_MODE_KEY]?.let(::parseSortMode)
                ?: defaultValue.albumSortMode,
        )
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        output.write(
            buildString {
                append(PIN_HIDDEN_KEY)
                append('=')
                append(t.pinHiddenAlbumsToTop)
                append('\n')
                append(SORT_MODE_KEY)
                append('=')
                append(t.albumSortMode.name)
            }.toByteArray(StandardCharsets.UTF_8),
        )
    }

    private fun parseBooleanSetting(rawValue: String): Boolean {
        return when (rawValue) {
            "true", "1" -> true
            "false", "0" -> false
            else -> throw CorruptionException("Cannot read user settings")
        }
    }

    private fun parseSortMode(rawValue: String): AlbumSortMode {
        return try {
            AlbumSortMode.valueOf(rawValue)
        } catch (_: IllegalArgumentException) {
            throw CorruptionException("Cannot read user settings")
        }
    }
}
