@file:Suppress("UNUSED_PARAMETER")

package explode.schema

import com.expediagroup.graphql.server.operations.Mutation
import explode.schema.model.AccessData
import explode.schema.model.UserModel
import kotlin.random.Random

object UserMutationService : Mutation {

	val FakeUserModel = UserModel(
		"testId",
		"FakeUsername-${Random.nextInt()}",
		listOf(),
		listOf(),
		listOf(),
		0,
		-9999,
		9999,
		"20790510T000000",
		"trash-potato-server-but-online",
		0,
		AccessData(false)
	)

	// TODO: Implementation
	suspend fun loginUser(username: String?, password: String?): UserModel {
		return FakeUserModel
	}

	// TODO: Implementation
	suspend fun registerUser(username: String?, password: String?): UserModel {
		return FakeUserModel
	}
}