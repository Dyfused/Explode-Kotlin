import explode.backend.bomb.OkResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class TestJson {
	@Test
	fun testJson() {

		val a = OkResult(DataObject("TEST"))

		println(Json.encodeToString(a))
	}

	@Serializable
	data class DataObject(val data: String)
}