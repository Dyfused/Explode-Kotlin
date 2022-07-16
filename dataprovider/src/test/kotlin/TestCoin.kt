import explode.dataprovider.model.PlayModInput
import explode.dataprovider.model.PlayRecordInput
import explode.dataprovider.provider.mongo.Detonate
import org.junit.jupiter.api.Test

class TestCoin {

	@Test
	fun testCoin() {
		val diff = 10

		val perf = 1200
		val good = 20
		val miss = 8

		val playRecordData = PlayRecordInput(
			mod = PlayModInput(0.0, 0.0, isBleed = true, isMirror = false),
			true, 0, perf, good, miss
		)

		println(Detonate.calcGainCoin(true, diff, playRecordData))
	}

}