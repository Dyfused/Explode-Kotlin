import explode.dataprovider.model.database.MongoChart
import explode.dataprovider.provider.compareCharts
import explode.dataprovider.provider.mongo.randomId
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TestChartCompare {

	@Test
	fun testChartCompare() {

		val c1 = MongoChart(randomId(), 1, Random.nextInt(0, 17), null)
		val c2 = MongoChart(randomId(), 2, Random.nextInt(0, 17), null)
		val c3 = MongoChart(randomId(), 3, Random.nextInt(0, 17), null)
		val c4 = MongoChart(randomId(), 4, Random.nextInt(0, 17), null)

		val s1 = listOf(c1, c2, c3, c4)
		val s2 = listOf(c1, c2, c3, c4)
		val s3 = listOf(c1, c3, c4)

		println(compareCharts(s1, s2))
		println(compareCharts(s2, s1))
		println(compareCharts(s1, s3))
		println(compareCharts(s3, s1))

	}

}