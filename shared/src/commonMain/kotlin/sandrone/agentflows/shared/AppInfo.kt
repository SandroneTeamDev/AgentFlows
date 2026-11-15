package sandrone.agentflows.shared

object AppInfo {
    const val Version = "v0.0.1-rc1-alpha3"
    val Channel: String get() = Version.substringAfterLast('-')
}
