package com.example.exchangededivisas.data.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class NativeNotificationEvent(
    val recipientUserId: Int?,
    val notificationId: Int?,
    val title: String,
    val body: String
)

object NativeNotificationBus {
    private val _events = MutableSharedFlow<NativeNotificationEvent>(
        replay = 0,
        extraBufferCapacity = 0
    )

    val events = _events.asSharedFlow()

    suspend fun emitForUser(
        recipientUserId: Int,
        title: String,
        body: String,
        notificationId: Int? = null
    ) {
        _events.emit(
            NativeNotificationEvent(
                recipientUserId = recipientUserId,
                notificationId = notificationId,
                title = title.take(80),
                body = body.take(240)
            )
        )
    }

    suspend fun emitBroadcast(
        title: String,
        body: String
    ) {
        _events.emit(
            NativeNotificationEvent(
                recipientUserId = null,
                notificationId = null,
                title = title.take(80),
                body = body.take(240)
            )
        )
    }

    fun clear() {
        // No hay replay ni cola local persistente. Este método existe para
        // centralizar el cierre de sesión y evitar dependencias directas desde la UI.
    }
}
