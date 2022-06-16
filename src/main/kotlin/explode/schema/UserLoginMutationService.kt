@file:Suppress("UNUSED_PARAMETER")

package explode.schema

import com.expediagroup.graphql.server.operations.Mutation
import explode.schema.model.AccessData
import explode.schema.model.UserModel

class UserLoginMutationService : Mutation {

	companion object {
		internal val FakeUserModel = UserModel(
			"testId",
			"FakeUsername",
			listOf(),
			listOf(),
			listOf(),
			0,
			0,
			0,
			"20790510T000000",
			"trash-potato-server",
			0,
			AccessData(false)
		)
	}

	// TODO: Implementation
	suspend fun loginUser(username: String?, password: String?): UserModel {
		return FakeUserModel
	}

	// TODO: Implementation
	suspend fun registerUser(username: String?, password: String?): UserModel {
		return FakeUserModel
	}
}