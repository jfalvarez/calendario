package com.jfaf.calendario.data.remote

import com.jfaf.calendario.domain.model.AppVersionPolicy
import com.jfaf.calendario.domain.repository.AppConfigRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/** Lee la fila de `app_config` de la plataforma. Fila inexistente → `null`; errores de red se propagan al caso de uso. */
class SupabaseAppConfigRepository(
    private val client: SupabaseClient,
    private val platform: AppPlatform,
) : AppConfigRepository {

    override suspend fun fetchPolicy(): AppVersionPolicy? =
        client.from("app_config")
            .select { filter { eq("platform", platform.id) } }
            .decodeSingleOrNull<RemoteAppConfig>()
            ?.toPolicy()
}

internal fun RemoteAppConfig.toPolicy() = AppVersionPolicy(
    minVersionCode = minVersionCode,
    recommendedVersionCode = recommendedVersionCode,
    message = message?.takeIf { it.isNotBlank() },
    storeUrl = storeUrl?.takeIf { it.isNotBlank() },
)
