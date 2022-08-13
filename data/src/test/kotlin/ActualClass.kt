import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

data class ActualClass(var a: Int, var b: String)

class WrappingActualClass(private val actualClass: ActualClass) {

	val c by sync(actualClass.a) { actualClass.a = it }

}

inline fun <T> sync(initial: T, crossinline doSync: (T) -> Unit): ReadWriteProperty<Any?, T> = Delegates.observable(initial) { _, _, newValue -> doSync(newValue) }