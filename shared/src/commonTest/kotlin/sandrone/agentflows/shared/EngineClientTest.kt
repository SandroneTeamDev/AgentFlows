package sandrone.agentflows.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class EngineClientTest {

    @Test
    fun emptyRuntimeIsNotPresentedAsACompletedAgentTask() {
        val snapshot = parseSnapshot(
            Json.parseToJsonElement(
                """{"status":{"running":true},"modelConfigured":false,"flows":[],"workers":[],"tasks":[],"usage":[],"providers":[],"uptimeMs":1200}""",
            ),
        )
        assertTrue(snapshot.connected)
        assertFalse(snapshot.modelConfigured)
        assertTrue(snapshot.flows.isEmpty())
        assertTrue(snapshot.workers.isEmpty())
        assertTrue(snapshot.tasks.isEmpty())
        assertEquals(1200L, snapshot.uptimeMs)
    }

    @Test
    fun preservesInspectableToolArgumentsAndResults() {
        val snapshot = parseSnapshot(
            Json.parseToJsonElement(
                """{"modelConfigured":true,"flows":[{"id":"event","type":"tool_call","label":"read file","timestamp":10,"data":{"args":{"path":"README.md"},"result":"text"}}],"workers":[{"id":"w1","status":"running"}],"tasks":[],"usage":[],"providers":[],"uptimeMs":10}""",
            ),
        )
        assertEquals("event", snapshot.flows.single().id)
        assertTrue(snapshot.flows.single().details.contains("README.md"))
        assertTrue(snapshot.modelConfigured)
        assertEquals("w1", snapshot.workers.single().id)
    }

    @Test
    fun parsesUsageProvidersAndWatchdogState() {
        val snapshot = parseSnapshot(
            Json.parseToJsonElement(
                """{"modelConfigured":true,"flows":[],"workers":[],"tasks":[],"uptimeMs":5,
                   "usage":[{"sessionId":"s1","model":"gpt","provider":"openai","tokensIn":120,"tokensOut":30,"cost":0.42,"timestamp":99}],
                   "providers":[{"id":"local","kind":"openai","baseUrl":"http://127.0.0.1:11434/v1","models":["a","b"]}],
                   "watchdog":{"healthy":false,"alerts":["worker w1 stalled"],"lastHeartbeat":7}}""",
            ),
        )
        val usage = snapshot.usage.single()
        assertEquals(120L, usage.tokensIn)
        assertEquals(30L, usage.tokensOut)
        assertEquals(0.42, usage.cost)
        assertEquals(listOf("a", "b"), snapshot.providers.single().models)
        assertFalse(snapshot.watchdog.healthy)
        assertEquals(listOf("worker w1 stalled"), snapshot.watchdog.alerts)
    }

    @Test
    fun submitResultSplitsTriggerAndState() {
        val parsed = parseSubmitResult(
            Json.parseToJsonElement("""{"triggerId":"abc","state":"pending_configuration"}"""),
        )
        assertEquals("abc|pending_configuration", parsed)
    }

    @Test
    fun watchdogAnswerFallsBackToMessage() {
        assertEquals("all good", parseWatchdogAnswer(Json.parseToJsonElement("""{"answer":"all good"}""")))
        assertEquals("ok", parseWatchdogAnswer(Json.parseToJsonElement("""{"message":"ok"}""")))
        assertEquals("", parseWatchdogAnswer(Json.parseToJsonElement("""{}""")))
    }

    @Test
    fun probeResultDefaultsToFalseWhenMissing() {
        assertTrue(parseProbeResult(Json.parseToJsonElement("""{"healthy":true}""")))
        assertFalse(parseProbeResult(Json.parseToJsonElement("""{}""")))
    }

    @Test
    fun effectPrefsRoundTripsThroughItsWireFormat() {
        val prefs = EffectPrefs(mode = EffectMode.GLASS, blurRadiusDp = 24f, glassTintAlpha = 0.5f)
        val restored = decodeEffectPrefs(prefs.encode())
        assertEquals(EffectMode.GLASS, restored.mode)
        assertEquals(24f, restored.blurRadiusDp)
        assertEquals(0.5f, restored.glassTintAlpha)
    }

    @Test
    fun effectPrefsFallsBackOnGarbageInput() {
        val restored = decodeEffectPrefs("not-a-valid-payload")
        assertEquals(EffectMode.DISABLED, restored.mode)
        assertEquals(12f, restored.blurRadiusDp)
        val blank = decodeEffectPrefs(null)
        assertEquals(EffectMode.DISABLED, blank.mode)
    }

    @Test
    fun parsesUsageSummaryTotalsAndBreakdowns() {
        val summary = parseUsageSummary(
            Json.parseToJsonElement(
                """{"range":"7d",
                   "totals":{"tokens":900,"tokensIn":600,"tokensOut":300,"cacheHits":120,"cost":1.25,"requests":3,"priced":false},
                   "composition":{"input":600,"output":300,"cache":120},
                   "byDay":[{"date":"2026-11-13","tokens":900,"cost":1.25,"requests":3}],
                   "bySession":[{"sessionId":"s1","models":["gpt","local"],"tokens":900,"tokensIn":600,"tokensOut":300,"cost":1.25,"requests":3,"lastActivity":77}],
                   "byModel":[{"model":"gpt","provider":"openai","tokens":900,"cost":1.25,"requests":3}],
                   "heatmap":[{"date":"2026-11-13","tokens":900,"cost":1.25,"requests":3}]}""",
            ),
        )
        assertEquals("7d", summary.range)
        assertEquals(900L, summary.totals.tokens)
        assertEquals(1.25, summary.totals.cost)
        assertFalse(summary.priced)
        assertEquals("2026-11-13", summary.byDay.single().date)
        assertEquals(listOf("gpt", "local"), summary.bySession.single().models)
        assertEquals("openai", summary.byModel.single().provider)
        assertEquals(3, summary.heatmap.single().requests)
    }

    @Test
    fun usageSummarySurvivesMissingFields() {
        val summary = parseUsageSummary(Json.parseToJsonElement("""{}"""))
        assertEquals(0L, summary.totals.tokens)
        assertEquals(0.0, summary.totals.cost)
        assertTrue(summary.priced)
        assertTrue(summary.bySession.isEmpty())
        assertTrue(summary.heatmap.isEmpty())
    }

    @Test
    fun durationAndCountFormattingStayReadable() {
        assertEquals("0s", sandrone.agentflows.shared.ui.formatDuration(0))
        assertEquals("45s", sandrone.agentflows.shared.ui.formatDuration(45_000))
        assertEquals("2m 5s", sandrone.agentflows.shared.ui.formatDuration(125_000))
        assertEquals("3h 4m", sandrone.agentflows.shared.ui.formatDuration(11_040_000))
        assertEquals("1.2K", sandrone.agentflows.shared.ui.formatCount(1234))
        assertEquals("$1.50", sandrone.agentflows.shared.ui.formatCost(1.5))
    }
}
