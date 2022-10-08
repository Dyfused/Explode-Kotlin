package explode.dataprovider.model.database

import kotlinx.serialization.Serializable

@Serializable
enum class SetStatus {
	UNRANKED,
	RANKED,
	OFFICIAL,
	@Deprecated("use isReviewing instead")
	NEED_REVIEW,
	@Deprecated("use isHidden instead")
	HIDDEN;

	val isRanked: Boolean get() = this == RANKED || this == OFFICIAL

	val humanizedName by lazy {
		name.split('_').joinToString(separator = " ") {
			it.lowercase().replaceFirstChar(Char::titlecase)
		}
	}
}