package com.jfaf.calendario.data.remote

import com.jfaf.calendario.domain.repository.SocialError
import io.github.jan.supabase.exceptions.RestException

/** Traduce el mensaje de Postgres (código de `social.sql`) a un error de dominio. */
internal fun socialErrorOf(e: Throwable): SocialError {
    val text = when (e) {
        is RestException -> "${e.error} ${e.description.orEmpty()}"
        else -> e.message.orEmpty()
    }.lowercase()
    return when {
        "alias_taken" in text -> SocialError.AliasTaken
        "alias_locked" in text -> SocialError.AliasLocked
        "shift_missing" in text -> SocialError.ShiftMissing
        "assignment_missing" in text -> SocialError.AssignmentMissing
        "assignment_changed" in text -> SocialError.AssignmentChanged
        "target_busy" in text -> SocialError.TargetBusy
        "proposer_busy" in text -> SocialError.ProposerBusy
        "link_revoked" in text -> SocialError.LinkRevoked
        "swap_not_found" in text -> SocialError.SwapNotFound
        "invalid_alias" in text -> SocialError.InvalidAlias
        "not_authenticated" in text -> SocialError.NotAuthenticated
        "invalid_transition" in text -> SocialError.InvalidTransition
        "forbidden" in text -> SocialError.Forbidden
        "link_not_found" in text -> SocialError.LinkNotFound
        looksOffline(e) -> SocialError.Network
        else -> SocialError.Unknown
    }
}
