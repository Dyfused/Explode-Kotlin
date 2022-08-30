package explode.dataprovider.model.database

import kotlin.reflect.KProperty

enum class RecordSort(val prop: KProperty<*>) {
	TIME(MongoRecord::uploadedTime),
	SCORE(MongoRecord::score);
}

enum class StoreSort {
	PLAY_COUNT,
	PUBLISH_TIME
}