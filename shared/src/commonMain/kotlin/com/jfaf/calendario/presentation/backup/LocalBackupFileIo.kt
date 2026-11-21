package com.jfaf.calendario.presentation.backup

import androidx.compose.runtime.staticCompositionLocalOf
import com.jfaf.calendario.domain.platform.BackupFileIo

/** Lo aporta cada plataforma. Null donde no hay selector de ficheros (la sección de copia se oculta). */
val LocalBackupFileIo = staticCompositionLocalOf<BackupFileIo?> { null }
