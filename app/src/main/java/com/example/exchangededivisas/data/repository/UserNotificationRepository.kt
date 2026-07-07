package com.example.exchangededivisas.data.repository

import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.data.remote.NotificacionDto
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

object UserNotificationRepository {
    private val api = ApiClient.supabase

    suspend fun getRecentNotifications(usuarioId: Int, limit: Int = 20): List<NotificacionDto> {
        if (usuarioId <= 0) return emptyList()
        return api.getNotificacionesByUsuario(
            usuarioId = "eq.$usuarioId",
            limit = limit
        )
    }

    fun observeNewNotificationsForUser(
        usuarioId: Int,
        ignoreExisting: Boolean = true,
        pollDelayMs: Long = 15_000L
    ): Flow<NotificacionDto> = flow {
        if (usuarioId <= 0) return@flow

        var lastSeenId = if (ignoreExisting) {
            getLatestNotificationId(usuarioId)
        } else {
            0
        }

        while (currentCoroutineContext().isActive) {
            val newNotifications = api.getUserNotificationsAfter(
                usuarioId = "eq.$usuarioId",
                notificacionId = "gt.$lastSeenId"
            )

            newNotifications
                .sortedBy { it.notificacionId ?: 0 }
                .forEach { notification ->
                    val id = notification.notificacionId ?: return@forEach
                    if (id > lastSeenId) {
                        emit(notification)
                        lastSeenId = id
                    }
                }

            delay(pollDelayMs)
        }
    }

    private suspend fun getLatestNotificationId(usuarioId: Int): Int {
        if (usuarioId <= 0) return 0
        return api.getLatestUserNotification(
            usuarioId = "eq.$usuarioId"
        ).firstOrNull()?.notificacionId ?: 0
    }
}
