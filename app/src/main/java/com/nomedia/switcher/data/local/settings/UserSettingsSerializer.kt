package com.nomedia.switcher.data.local.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object UserSettingsSerializer : Serializer<UserSettings> {
    override val defaultValue: UserSettings = UserSettings()

    override suspend fun readFrom(input: InputStream): UserSettings {
        return try {
            val payload = input.readBytes().toString(StandardCharsets.UTF_8).trim()
            if (payload.isEmpty()) {
                defaultValue
            } else {
                UserSettings(
                    pinHiddenAlbumsToTop = payload.substringAfter("=")
                        .trim()
                        .toBooleanStrictOrNull()
                        ?: payload.substringAfter("=").trim() == "1",
                )
            }
        } catch (error: IllegalArgumentException) {
            throw CorruptionException("Cannot read user settings", error)
        }
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        output.write(
            "pinHiddenAlbumsToTop=${t.pinHiddenAlbumsToTop}"
                .toByteArray(StandardCharsets.UTF_8),
        )
    }
}
