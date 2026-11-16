package com.jfaf.calendario.data.sync

import com.jfaf.calendario.data.local.LocalHolidayEntity
import com.jfaf.calendario.data.local.ShiftAssignmentEntity
import com.jfaf.calendario.data.local.ShiftEntity
import com.jfaf.calendario.data.remote.RemoteAssignment
import com.jfaf.calendario.data.remote.RemoteLocalHoliday
import com.jfaf.calendario.data.remote.RemoteShift

/**
 * Reglas de fusión («el último cambio gana» por registro). Devuelven la fila que hay que escribir en local,
 * o `null` si la local es más reciente y se conserva (si estaba pendiente, se enviará en el próximo push).
 * Con igual `updatedAt` se acepta la remota: es la que ya enviamos nosotros o una idéntica.
 */
internal fun mergeShift(local: ShiftEntity?, remote: RemoteShift): ShiftEntity? {
    if (local != null && local.updatedAt > remote.updatedAt) return null
    return ShiftEntity(
        id = remote.id,
        name = remote.name,
        abbreviation = remote.abbreviation,
        colorArgb = remote.colorArgb,
        sortOrder = remote.sortOrder,
        updatedAt = remote.updatedAt,
        deletedAt = remote.deletedAt,
        dirty = false,
        startMinutes = remote.startMinutes,
        endMinutes = remote.endMinutes,
        breakMinutes = remote.breakMinutes,
        // Columnas de pago reservadas (sin uso): se conservan tal cual.
        payMode = local?.payMode ?: "PerShift",
        payCents = local?.payCents ?: 0,
    )
}

internal fun mergeAssignment(local: ShiftAssignmentEntity?, remote: RemoteAssignment): ShiftAssignmentEntity? {
    if (local != null && local.updatedAt > remote.updatedAt) return null
    return ShiftAssignmentEntity(
        id = remote.id,
        date = remote.date,
        slot = remote.slot,
        shiftId = remote.shiftId,
        updatedAt = remote.updatedAt,
        deletedAt = remote.deletedAt,
        dirty = false,
    )
}

internal fun mergeHoliday(local: LocalHolidayEntity?, remote: RemoteLocalHoliday): LocalHolidayEntity? {
    if (local != null && local.updatedAt > remote.updatedAt) return null
    return LocalHolidayEntity(remote.date, remote.updatedAt, remote.deletedAt, dirty = false)
}

internal fun LocalHolidayEntity.toRemote(ownerId: String) = RemoteLocalHoliday(ownerId, date, updatedAt, deletedAt)

internal fun ShiftEntity.toRemote(ownerId: String) = RemoteShift(
    ownerId, id, name, abbreviation, colorArgb, sortOrder, startMinutes, endMinutes, breakMinutes, updatedAt, deletedAt,
)

internal fun ShiftAssignmentEntity.toRemote(ownerId: String) =
    RemoteAssignment(ownerId, id, date, slot, shiftId, updatedAt, deletedAt)
