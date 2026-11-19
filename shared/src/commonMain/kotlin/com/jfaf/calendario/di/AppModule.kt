package com.jfaf.calendario.di

/** Módulos de Koin de la app. Cada plataforma añade lo suyo (base de datos, DataStore, `SupabaseConfig`). */
val appModules = listOf(dataModule, domainModule, presentationModule)
