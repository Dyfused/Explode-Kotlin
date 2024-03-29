package explode.dataprovider.model.database

import kotlinx.serialization.Serializable

@Serializable
enum class SetStatus {
	UNRANKED,
	RANKED,
	OFFICIAL,
	NEED_REVIEW,
	HIDDEN;

	val isRanked: Boolean get() = this == RANKED || this == OFFICIAL

	val humanizedName by lazy {
		name.split('_').joinToString(separator = " ") {
			it.lowercase().replaceFirstChar(Char::titlecase)
		}
	}
}