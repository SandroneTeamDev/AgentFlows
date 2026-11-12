package sandrone.agentflows.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class EngineClientTest {
    @Test
    fun emptyRuntimeIsNotPresentedAsACompletedAgentTask() {
        val snapshot = parseSnapshot(Json.parseToJsonElement("""{"status":{"running":true,"modelConfigured":false},"flows":[],"tasks":[]}"""))
        assertTrue(snapshot.connected)
        assertFalse(snapshot.modelConfigured)
        assertTrue(snapshot.flows.isEmpty())
        assertTrue(snapshot.tasks.isEmpty())
    }

    @Test
    fun preservesInspectableToolArgumentsAndResults() {
        val snapshot = parseSnapshot(Json.parseToJsonElement("""{"status":{"modelConfigured":true},"flows":[{"id":"event","type":"tool_call","label":"read file","timestamp":10,"data":{"args":{"path":"README.md"},"result":"text"}}],"tasks":[]}"""))
        assertEquals("event", snapshot.flows.single().id)
        assertTrue(snapshot.flows.single().details.contains("README.md"))
        assertTrue(snapshot.modelConfigured)
    }
}
