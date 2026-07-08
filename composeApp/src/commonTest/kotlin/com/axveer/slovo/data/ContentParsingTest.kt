package com.axveer.slovo.data

import com.axveer.slovo.domain.LessonKind
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentParsingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun parses_unit_file() {
        val text = """
          {"lessons":[{"id":"l1","title":"Greetings","kind":"VOCAB","cardIds":["c1"]}],
           "cards":[{"id":"c1","russian":"привет","transliteration":"privet","english":"hello","audio":"c1.m4a"}]}
        """.trimIndent()
        val unit = parseUnitFile(json, text)
        assertEquals(1, unit.lessons.size)
        assertEquals(LessonKind.VOCAB, unit.lessons[0].kind)
        assertEquals("привет", unit.cards[0].russian)
    }

    @Test fun parses_manifest() {
        val text = """{"units":[{"id":"u1","title":"Basics","lessonCount":2}]}"""
        val metas = parseManifest(json, text)
        assertEquals("u1", metas[0].id)
        assertEquals(2, metas[0].lessonCount)
    }
}
