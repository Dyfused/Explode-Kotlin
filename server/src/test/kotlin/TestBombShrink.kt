import explode.backend.bomb.v1.backend.bombify
import explode.dataprovider.model.database.*
import explode.dataprovider.model.newUUID
import explode.dataprovider.provider.mongo.randomId
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class TestBombShrink {

	private val user = MongoUser(
		newUUID(),
		"TestUserName",
		"TestUserPassword",
		mutableListOf(),
		mutableListOf(),
		25565,
		32767,
		OffsetDateTime.now(),
		randomId(),
		999,
		UserPermission.Administrator,
		2
	)

	@Test
	fun testUserShrink() {
		println(user.bombify())
	}

	@Test
	fun testProper() {
		println(
			MongoRecord(
				newUUID(),
				newUUID(),
				randomId(),
				1000000,
				ScoreDetail(1000, 0, 0),
				OffsetDateTime.now(),
				592.23
			).bombify()
		)
	}

}