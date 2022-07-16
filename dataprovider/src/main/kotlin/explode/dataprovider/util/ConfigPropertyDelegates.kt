package explode.dataprovider.util

import TConfig.Property
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object ConfigPropertyDelegates {
	fun Property.delegateString() = object : ReadWriteProperty<Any?, String> {
		override fun getValue(thisRef: Any?, property: KProperty<*>): String {
			return this@delegateString.string
		}

		override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
			this@delegateString.setValue(value)
		}
	}

	fun Property.delegateBoolean() = object : ReadWriteProperty<Any?, Boolean> {
		override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
			return this@delegateBoolean.boolean
		}

		override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
			this@delegateBoolean.setValue(value)
		}
	}

	fun Property.delegateInt() = object : ReadWriteProperty<Any?, Int> {
		override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
			return this@delegateInt.int
		}

		override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
			this@delegateInt.setValue(value)
		}
	}
}