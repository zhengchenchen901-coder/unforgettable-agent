package com.unforgettable.memory.data.storage.converter

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class RoomTypeConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @TypeConverter
    fun encodeStringList(value: List<String>?): String {
        return json.encodeToString(ListSerializer(String.serializer()), value.orEmpty())
    }

    @TypeConverter
    fun decodeStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), value)
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun encodeLongList(value: List<Long>?): String {
        return json.encodeToString(ListSerializer(Long.serializer()), value.orEmpty())
    }

    @TypeConverter
    fun decodeLongList(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(Long.serializer()), value)
        }.getOrDefault(emptyList())
    }
}
