package explode.dataprovider.provider

import explode.dataprovider.model.UserModel

interface IBlowUserAccessor {

	fun getUser(userId: String): UserModel?

	fun getUserByName(username: String): UserModel?

	fun getUserByToken(soudayo: String): UserModel?

	/**
	 * example:
	 * ```
	 * with(provider) {
	 * 	println(user.password)
	 * 	user.password = "123"
	 * }
	 * ```
	 */
	var UserModel.password: String

	val emptyUser: UserModel
}