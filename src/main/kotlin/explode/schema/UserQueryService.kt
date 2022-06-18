@file:Suppress("UNUSED_PARAMETER")

package explode.schema

import com.expediagroup.graphql.server.operations.Query
import explode.schema.model.UserModel

object UserQueryService : Query {

	// TODO: Implementation
	suspend fun userByUsername(username: String?): UserModel {
		return UserMutationService.FakeUserModel
	}

}