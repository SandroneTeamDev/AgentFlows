package sandrone.agentflows.shared

import java.util.UUID

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun randomToken(): String = UUID.randomUUID().toString()
