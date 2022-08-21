package explode.dataprovider.model.database

import kotlinx.serialization.Serializable

@Serializable
data class UserPermission(
	/**
	 * Permissions to manage the Review things. Also has the permission to view the 'Review' category of the Store.
	 */
	var review: Boolean = false,

	/**
	 * Permissions to manage the database and to view the sensitive data.
	 */
	var operator: Boolean = false,
) {
	companion object {
		val Default = UserPermission()
		val Administrator = UserPermission(review = true, operator = true)
	}
}
