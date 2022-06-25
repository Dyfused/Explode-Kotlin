package explode.backend

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal sealed interface RequestResult

internal data class SuccessResult<T, E>(val data: T, val extra: E?) : RequestResult

internal data class FailureResult<T, E>(val error: T, val extra: E?) : RequestResult

internal fun RequestResult.encodeToJson() = Json.encodeToString(this)