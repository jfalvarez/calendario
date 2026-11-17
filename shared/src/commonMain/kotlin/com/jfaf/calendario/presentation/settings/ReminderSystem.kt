package com.jfaf.calendario.presentation.settings

/** Lo que los avisos necesitan del sistema operativo; cada plataforma aporta el suyo. */
interface ReminderSystem {
    /** `false` si la plataforma no puede emitir avisos (escritorio): la sección se oculta en Ajustes. */
    val supported: Boolean

    /** `true` si se pueden mostrar notificaciones (en Android 13+ exige el permiso del usuario). */
    fun notificationsAllowed(): Boolean

    /** Pide el permiso de notificaciones si hace falta y devuelve el resultado por [onResult]. */
    fun requestNotifications(onResult: (granted: Boolean) -> Unit)
}
