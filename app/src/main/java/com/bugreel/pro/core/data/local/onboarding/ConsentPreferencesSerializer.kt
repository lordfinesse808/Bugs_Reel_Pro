package com.bugreel.pro.core.data.local.onboarding

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// CLAUDE.md Section 2 lists this store as "DataStore (Proto)". This is a typed, single-object
// DataStore -- the same durability/atomicity guarantees Proto DataStore provides -- backed by a
// JSON-serialized Serializer<ConsentPreferences> instead of a compiled .proto schema, since the
// stored shape is two booleans and doesn't warrant adding the protobuf compiler toolchain.
object ConsentPreferencesSerializer : Serializer<ConsentPreferences> {

    override val defaultValue: ConsentPreferences = ConsentPreferences()

    override suspend fun readFrom(input: InputStream): ConsentPreferences =
        try {
            Json.decodeFromString(input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read ConsentPreferences", e)
        }

    override suspend fun writeTo(t: ConsentPreferences, output: OutputStream) {
        output.write(Json.encodeToString(t).encodeToByteArray())
    }
}
