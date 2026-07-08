package com.axveer.slovo.data

import com.axveer.slovo.domain.Card
import com.axveer.slovo.domain.LearnUnit
import com.axveer.slovo.domain.Lesson
import com.axveer.slovo.domain.UnitMeta
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import slovo.composeapp.generated.resources.Res

const val MIXED_ID = "mixed"

@Serializable internal data class ManifestEntry(val id: String, val title: String, val lessonCount: Int)
@Serializable internal data class Manifest(val units: List<ManifestEntry>)
@Serializable internal data class UnitFile(val lessons: List<Lesson>, val cards: List<Card>)

internal fun parseManifest(json: Json, text: String): List<UnitMeta> =
    json.decodeFromString<Manifest>(text).units.map { UnitMeta(it.id, it.title, it.lessonCount) }

internal fun parseUnitFile(json: Json, text: String): UnitFile =
    json.decodeFromString<UnitFile>(text)

class ContentRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private var metaCache: List<UnitMeta>? = null
    private val unitCache = mutableMapOf<String, LearnUnit>()

    suspend fun units(): List<UnitMeta> {
        metaCache?.let { return it }
        val text = Res.readBytes("files/content/manifest.json").decodeToString()
        return parseManifest(json, text).also { metaCache = it }
    }

    suspend fun unit(unitId: String): LearnUnit {
        unitCache[unitId]?.let { return it }
        val meta = units().first { it.id == unitId }
        val text = Res.readBytes("files/content/$unitId.json").decodeToString()
        val file = parseUnitFile(json, text)
        return LearnUnit(meta, file.lessons, file.cards).also { unitCache[unitId] = it }
    }

    suspend fun allCards(): List<Card> =
        units().flatMap { unit(it.id).cards }.distinctBy { it.id }
}
