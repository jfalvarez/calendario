package com.jfaf.calendario.data.remote

/**
 * URL del proyecto y clave **pública** (`anon` / `publishable`) de Supabase. Cada plataforma la lee de
 * `local.properties` en la compilación. Sin configurar, la app funciona sólo en local.
 */
data class SupabaseConfig(val url: String, val anonKey: String) {
    val isConfigured: Boolean get() = url.startsWith("https://") && anonKey.isNotBlank()
}
