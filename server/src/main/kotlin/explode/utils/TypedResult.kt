@file:Suppress("UNCHECKED_CAST")
@file:OptIn(ExperimentalContracts::class)

package explode.utils

import java.io.Serializable
import kotlin.contracts.*

class TypedResult<out T, out E> internal constructor(val isSuccess: Boolean, internal val value: Any?) : Serializable {

	fun getOrNull(): T? = when {
		isSuccess -> value as T
		else -> null
	}

	fun exceptionOrNull(): E? = when {
		isSuccess -> null
		else -> value as E
	}

	companion object {
		fun <T, E> success(value: T) = TypedResult<T, E>(true, value)
		fun <T, E> failure(value: E) = TypedResult<T, E>(false, value)
	}
}

fun <T, E> TypedResult<T, E>.onSuccess(action: (value: T) -> Unit): TypedResult<T, E> {
	contract {
		callsInPlace(action, InvocationKind.AT_MOST_ONCE)
	}
	if(isSuccess) action(value as T)
	return this
}

fun <T, E> TypedResult<T, E>.onFailure(action: (value: E) -> Unit): TypedResult<T, E> {
	contract {
		callsInPlace(action, InvocationKind.AT_MOST_ONCE)
	}
	if(!isSuccess) action(value as E)
	return this
}

fun <T, E> TypedResult<T, E>.getOrThrow(throwable: () -> Throwable = ::NullPointerException): T {
	contract {
		callsInPlace(throwable, InvocationKind.AT_MOST_ONCE)
	}
	if(isSuccess) {
		return value as T
	} else {
		val thr = throwable()
		throw thr
	}
}