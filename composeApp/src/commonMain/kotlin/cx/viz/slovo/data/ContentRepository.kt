package cx.viz.slovo.data

import cx.viz.slovo.domain.Card
import cx.viz.slovo.domain.LearnUnit
import cx.viz.slovo.domain.Lesson
import cx.viz.slovo.domain.LessonKind
import cx.viz.slovo.domain.UnitMeta
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import slovo.composeapp.generated.resources.Res

const val MIXED_ID = "mixed"

// Wire DTOs keep kotlinx.serialization out of the domain layer; they map to plain
// domain models on read so nothing downstream depends on the JSON shape.
@Serializable internal data class ManifestEntry(val id: String, val title: String, val lessonCount: Int)
@Serializable internal data class Manifest(val units: List<ManifestEntry>)

@Serializable internal data class CardDto(
    val id: String,
    val russian: String,
    val transliteration: String,
    val english: String,
    val audio: String,
    val note: String? = null,
) {
    fun toDomain() = Card(id, russian, transliteration, english, audio, note)
}

@Serializable internal data class LessonDto(
    val id: String,
    val title: String,
    val kind: LessonKind,
    val cardIds: List<String>,
) {
    fun toDomain() = Lesson(id, title, kind, cardIds)
}

@Serializable internal data class UnitFile(val lessons: List<LessonDto>, val cards: List<CardDto>)

internal fun parseManifest(json: Json, text: String): List<UnitMeta> =
    json.decodeFromString(Manifest.serializer(), text).units.map { UnitMeta(it.id, it.title, it.lessonCount) }

internal fun parseUnitFile(json: Json, text: String): UnitFile =
    json.decodeFromString(UnitFile.serializer(), text)

/**
 * Read access to the bundled learning content. An interface so screens can be
 * driven by fake content in tests; production uses [BundledContentRepository].
 */
interface ContentRepository {
    suspend fun units(): List<UnitMeta>
    suspend fun unit(unitId: String): LearnUnit
    suspend fun allCards(): List<Card>
}

class BundledContentRepository : ContentRepository {
    // useAlternativeNames = false skips the @JsonNames annotation lookup that
    // null-derefs in kotlinx.serialization on Kotlin/Native (iOS).
    private val json = Json { ignoreUnknownKeys = true; useAlternativeNames = false }
    private val cacheMutex = Mutex()
    private var metaCache: List<UnitMeta>? = null
    private val unitCache = mutableMapOf<String, LearnUnit>()

    override suspend fun units(): List<UnitMeta> {
        cacheMutex.withLock { metaCache }?.let { return it }
        val text = Res.readBytes("files/content/manifest.json").decodeToString()
        return parseManifest(json, text).also { cacheMutex.withLock { metaCache = it } }
    }

    override suspend fun unit(unitId: String): LearnUnit {
        cacheMutex.withLock { unitCache[unitId] }?.let { return it }
        val meta = units().first { it.id == unitId }
        val text = Res.readBytes("files/content/$unitId.json").decodeToString()
        val file = parseUnitFile(json, text)
        val unit = LearnUnit(meta, file.lessons.map { it.toDomain() }, file.cards.map { it.toDomain() })
        return unit.also { cacheMutex.withLock { unitCache[unitId] = it } }
    }

    override suspend fun allCards(): List<Card> =
        units().flatMap { unit(it.id).cards }.distinctBy { it.id }
}
