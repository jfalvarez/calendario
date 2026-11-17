package com.jfaf.calendario.domain.repository

/** Versión instalada; cada plataforma la aporta desde su compilación. */
interface AppVersionProvider {
    val versionCode: Int
    val versionName: String
}
