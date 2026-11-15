package sandrone.agentflows.shared.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val tag: String, val nativeLabel: String) {
    ZH("zh", "中文"),
    EN("en", "English"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: ZH
    }
}

object AppLocale {
    var language: AppLanguage by mutableStateOf(AppLanguage.ZH)

    val chinese: Boolean get() = language == AppLanguage.ZH
}

fun pick(chinese: String, english: String): String = if (AppLocale.chinese) chinese else english
