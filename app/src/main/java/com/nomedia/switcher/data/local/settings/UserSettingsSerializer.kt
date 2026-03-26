package com.nomedia.switcher.data.local.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object UserSettingsSerializer : Serializer<UserSettings> {
    override val defaultValue: UserSettings = UserSettings()

    override suspend fun readFrom(input: InputStream): UserSettings {
        val payload = input.readBytes().toString(StandardCharsets.UTF_8).trim()
        if (payload.isEmpty()) {
            return defaultValue
        }

        val separatorIndex = payload.indexOf('=')
        if (separatorIndex <= 0) {
            throw CorruptionException("Cannot read user settings")
        }

        val key = payload.substring(0, separatorIndex).trim()
        val rawValue = payload.substring(separatorIndex + 1).trim()
        if (key != "pinHiddenAlbumsToTop") {
            throw CorruptionException("Cannot read user settings")
        }

        val value = when (rawValue) {
            "true", "1" -> true
            "false", "0" -> false
            else -> throw CorruptionException("Cannot read user settings")
        }

        return UserSettings(pinHiddenAlbumsToTop = value)
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        output.write(
            "pinHiddenAlbumsToTop=${t.pinHiddenAlbumsToTop}"
                .toByteArray(StandardCharsets.UTF_8),
        )
    }
}
