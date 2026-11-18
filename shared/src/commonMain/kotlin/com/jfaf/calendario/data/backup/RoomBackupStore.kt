package com.jfaf.calendario.data.backup

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.DayNoteEntity
import com.jfaf.calendario.data.local.LocalHolidayEntity
import com.jfaf.calendario.data.local.ShiftAssignmentEntity
import com.jfaf.calendario.data.local.ShiftEntity
import com.jfaf.calendario.data.repository.assignmentId
import com.jfaf.calendario.data.repository.nowMillis
import com.jfaf.calendario.domain.model.BackupAssignment
import com.jfaf.calendario.domain.model.BackupNote
import com.jfaf.calendario.domain.model.BackupShift
import com.jfaf.calendario.domain.model.BackupSnapshot
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.domain.repository.BackupStore
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/** Rango que cubre cualquier fecha que pueda tener la app; las consultas de copia leen todo. */
private const val FIRST = "0000-01-01"
private const val LAST = "9999-12-31"

/**
 * Lee y restaura los datos de Room. Restaurar marca **todo lo anterior como borrado** (con `dirty`), para que
 * la sincronización lo quite también del servidor, y da a lo nuevo un `updatedAt` actual para que gane el
 * último cambio. Todo ocurre en una sola transacción: si algo falla, no queda nada a medias.
 */
class RoomBackupStore(private val db: CalendarDatabase) : BackupStore {

    override suspend fun readAll(): BackupSnapshot = BackupSnapshot(
        shifts = db.shiftDao().getAlive().map { BackupShift(it.id, it.name, it.abbreviation, it.colorArgb, it.sortOrder, it.startMinutes, it.endMinutes, it.breakMinutes) },
        assignments = db.shiftAssignmentDao().getInRange(FIRST, LAST)
            .filter { it.deletedAt == null }
            .map { BackupAssignment(LocalDate.parse(it.date), it.slot, it.shiftId) },
        holidays = db.localHolidayDao().observeAlive(FIRST, LAST).first().map { LocalDate.parse(it) },
        notes = db.dayNoteDao().observeAlive(FIRST, LAST).first().map {
            BackupNote(it.id, LocalDate.parse(it.date), NoteKind.valueOf(it.kind), it.person, it.body, it.settled)
        },
    )

    override suspend fun replaceAll(snapshot: BackupSnapshot) {
        val now = nowMillis()
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                // 1. Lo actual pasa a borrado lógico (la sincronización lo propaga al servidor).
                db.shiftDao().upsertAll(db.shiftDao().getAlive().map { it.copy(deletedAt = now, updatedAt = now, dirty = true) })
                db.shiftAssignmentDao().upsertAll(
                    db.shiftAssignmentDao().getInRange(FIRST, LAST).filter { it.deletedAt == null }
                        .map { it.copy(deletedAt = now, updatedAt = now, dirty = true) },
                )
                db.localHolidayDao().upsertAll(
                    db.localHolidayDao().getByDates(db.localHolidayDao().observeAlive(FIRST, LAST).first())
                        .map { it.copy(deletedAt = now, updatedAt = now, dirty = true) },
                )
                db.dayNoteDao().upsertAll(
                    db.dayNoteDao().observeAlive(FIRST, LAST).first()
                        .map { it.copy(deletedAt = now, updatedAt = now, dirty = true) },
                )

                // 2. Lo de la copia entra vivo, con marca de cambio local.
                db.shiftDao().upsertAll(snapshot.shifts.map {
                    ShiftEntity(it.id, it.name, it.abbreviation, it.colorArgb, it.sortOrder, now, null, true, it.startMinutes, it.endMinutes, it.breakMinutes)
                })
                db.shiftAssignmentDao().upsertAll(snapshot.assignments.map {
                    val date = it.date.toString()
                    ShiftAssignmentEntity(assignmentId(date, it.slot), date, it.slot, it.shiftId, now, null, true)
                })
                db.localHolidayDao().upsertAll(snapshot.holidays.map { LocalHolidayEntity(it.toString(), now, null, true) })
                db.dayNoteDao().upsertAll(snapshot.notes.map {
                    DayNoteEntity(it.id, it.date.toString(), it.kind.name, it.person, it.text, it.settled, now, null, true)
                })
            }
        }
    }
}
