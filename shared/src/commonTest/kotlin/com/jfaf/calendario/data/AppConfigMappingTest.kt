package com.jfaf.calendario.data

import com.jfaf.calendario.data.remote.RemoteAppConfig
import com.jfaf.calendario.data.remote.toPolicy
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppConfigMappingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodificaFilaDeSupabaseYMapeaALaPolitica() {
        val row = json.decodeFromString<RemoteAppConfig>(
            """{"platform":"android","min_version_code":13,"recommended_version_code":14,
               "message":"Corrige un error","store_url":"https://play.google.com/x","updated_at":"2026-11-17T10:00:00Z"}""",
        )
        val policy = row.toPolicy()
        assertEquals(13, policy.minVersionCode)
        assertEquals(14, policy.recommendedVersionCode)
        assertEquals("Corrige un error", policy.message)
        assertEquals("https://play.google.com/x", policy.storeUrl)
    }

    @Test
    fun mensajeYUrlVaciosOAusentesSeConvierteEnNulo() {
        val policy = json.decodeFromString<RemoteAppConfig>(
            """{"platform":"desktop","min_version_code":1,"recommended_version_code":1,"message":"  ","store_url":""}""",
        ).toPolicy()
        assertNull(policy.message)
        assertNull(policy.storeUrl)
        assertNull(json.decodeFromString<RemoteAppConfig>("""{"platform":"desktop","min_version_code":1,"recommended_version_code":1}""").toPolicy().message)
    }
}
