package cx.viz.slovo.data

import cx.viz.slovo.domain.Card
import cx.viz.slovo.domain.LearnUnit
import cx.viz.slovo.domain.Lesson
import cx.viz.slovo.domain.UnitMeta
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import slovo.composeapp.generated.resources.Res

const val MIXED_ID = "mixed"

@Serializable internal data class ManifestEntry(val id: String, val title: String, val lessonCount: Int)
@Serializable internal data class Manifest(val units: List<ManifestEntry>)
@Serializable internal data class UnitFile(val lessons: List<Lesson>, val cards: List<Card>)

internal fun parseManifest(json: Json, text: String): List<UnitMeta> =
    json.decodeFromString(Manifest.serializer(), text).units.map { UnitMeta(it.id, it.title, it.lessonCount) }

internal fun parseUnitFile(json: Json, text: String): UnitFile =
    json.decodeFromString(UnitFile.serializer(), text)

class ContentRepository {
    // useAlternativeNames = false skips the @JsonNames annotation lookup that
    // null-derefs in kotlinx.serialization on Kotlin/Native (iOS).
    private val json = Json { ignoreUnknownKeys = true; useAlternativeNames = false }
    private val cacheMutex = Mutex()
    private var metaCache: List<UnitMeta>? = null
    private val unitCache = mutableMapOf<String, LearnUnit>()

    suspend fun units(): List<UnitMeta> {
        cacheMutex.withLock { metaCache }?.let { return it }
        val text = Res.readBytes("files/content/manifest.json").decodeToString()
        return parseManifest(json, text).also { cacheMutex.withLock { metaCache = it } }
    }

    suspend fun unit(unitId: String): LearnUnit {
        cacheMutex.withLock { unitCache[unitId] }?.let { return it }
        val meta = units().first { it.id == unitId }
        val text = Res.readBytes("files/content/$unitId.json").decodeToString()
        val file = parseUnitFile(json, text)
        return LearnUnit(meta, file.lessons, file.cards).also { cacheMutex.withLock { unitCache[unitId] = it } }
    }

    suspend fun allCards(): List<Card> =
        units().flatMap { unit(it.id).cards }.distinctBy { it.id }
}
