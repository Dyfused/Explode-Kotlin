package explode.dataprovider.model.database

import kotlinx.serialization.Serializable

@Serializable
data class UserPermission(
	var review: Boolean
) {
	companion object {
		val Default = UserPermission(false)
		val Administrator = UserPermission(true)
	}
}
