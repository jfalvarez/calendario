package com.jfaf.calendario

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.jfaf.calendario.domain.model.AppVersionPolicy
import com.jfaf.calendario.domain.repository.AppVersionProvider
import com.jfaf.calendario.domain.platform.UpdateLauncher

/**
 * Actualiza con Play In-App Updates: inmediato si la versión instalada está por debajo del mínimo, flexible si solo
 * hay una recomendada. Si Play no la ofrece (aún no propagada, instalación fuera de Play, error) abre la ficha de la tienda.
 *
 * La actividad se enlaza con [attach] antes de que empiece (el resultado se registra con `registerForActivityResult`).
 * En flexible no se llama a `completeUpdate()`: Play instala la descarga la próxima vez que la app pase a segundo plano,
 * así que no hace falta pedir un reinicio.
 */
class PlayUpdateLauncher(
    private val context: Context,
    private val version: AppVersionProvider,
) : UpdateLauncher {
    private val manager = AppUpdateManagerFactory.create(context)
    private var activity: ComponentActivity? = null
    private var resultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    /** Llamar en `onCreate` de la actividad (antes de STARTED). */
    fun attach(activity: ComponentActivity) {
        this.activity = activity
        resultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    }

    fun detach(activity: ComponentActivity) {
        if (this.activity === activity) {
            this.activity = null
            resultLauncher = null
        }
    }

    override fun startUpdate(policy: AppVersionPolicy) {
        val type = if (version.versionCode < policy.minVersionCode) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                val offered = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(type)
                if (!offered || !launchFlow(info, type)) openStore(policy)
            }
            .addOnFailureListener { openStore(policy) }
    }

    /** Un flujo inmediato interrumpido (la app se cerró a medias) debe reanudarse: llamar desde `onResume`. */
    fun resumeIfInProgress() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                launchFlow(info, AppUpdateType.IMMEDIATE)
            }
        }
    }

    private fun launchFlow(info: AppUpdateInfo, type: Int): Boolean {
        val launcher = resultLauncher ?: return false
        return runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        }.getOrDefault(false)
    }

    private fun openStore(policy: AppVersionPolicy) {
        val uri = (policy.storeUrl ?: "market://details?id=${context.packageName}").toUri()
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
