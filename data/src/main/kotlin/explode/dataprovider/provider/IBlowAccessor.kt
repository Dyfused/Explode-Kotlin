package explode.dataprovider.provider

import explode.dataprovider.model.database.*
import explode.dataprovider.model.game.*
import explode.dataprovider.util.applyIf

interface IBlowAccessor : IBlowReadOnly {

	val gameSetting: GameSettingModel

	fun loginUser(username: String, password: String): MongoUser

	fun registerUser(username: String, password: String): MongoUser

	fun getSets(
		limit: Int,
		skip: Int,
		filterName: String,
		filterCategory: StoreCategory,
		filterSort: StoreSort
	): List<MongoSet>

	fun getSets(limit: Int? = null, skip: Int? = null): Iterable<MongoSet>


	// updaters
	fun updateUser(mongoUser: MongoUser): MongoUser
	fun updateSet(mongoSet: MongoSet): MongoSet
	fun updateChart(mongoChart: MongoChart): MongoChart

	/**
	 * Create and store a new [User][UserModel].
	 *
	 * Note: [password] is plain text but not encrypted value. This is the security problem of Dynamite itself.
	 *
	 * @param username username
	 * @param password password
	 */
	fun createUser(username: String, password: String): MongoUser

	/**
	 * Create a Chart Set and store.
	 *
	 * @param musicName The title of the set showed in Dynamite like PUPA.
	 * @param composerName The music composer's name like モリモリあつし
	 * @param noterId The noter's display name, not have to be username.
	 * @param charts The charts of the set.
	 * @param status The status of the set.
	 * @param introduction Introduction texts. No usage in Dynamite.
	 * @param price The price to buy in the store.
	 */
	fun createSet(
		musicName: String,
		composerName: String,
		noterId: String?,
		charts: List<MongoChart>,
		defaultId: String? = null,
		introduction: String? = null,
		status: SetStatus = SetStatus.UNRANKED,
		price: Int = 0,
		displayNoterName: String? = null,

		musicContent: ByteArray? = null,
		previewMusicContent: ByteArray? = null,
		setCoverContent: ByteArray? = null,
		storePreviewContent: ByteArray? = null
	): MongoSet

	/**
	 * Create a Chart. Any chart required to be included in a [Set][createSet] to be available to Dynamite.
	 *
	 * @param difficultyClass The difficulty class, from 1 to 6 referring from CASUAL to TERA.
	 * @param difficultyValue The numeric value of difficulty.
	 * @param content the actual chart file. Stores to the specified directory if present.
	 */
	fun createChart(
		difficultyClass: Int,
		difficultyValue: Int,
		defaultId: String? = null,
		D: Double? = null,
		content: ByteArray? = null
	): MongoChart

	/**
	 * Build a set with charts with [ChartSetBuilder].
	 *
	 * @see createSet
	 * @see createChart
	 */
	fun buildChartSet(
		setTitle: String,
		composerName: String,
		noterUser: MongoUser?,
		coinPrice: Int,
		introduction: String = "",
		needReview: Boolean = true,
		defaultId: String? = null,
		expectStatus: SetStatus = SetStatus.UNRANKED,

		musicContent: ByteArray? = null,
		previewMusicContent: ByteArray? = null,
		setCoverContent: ByteArray? = null,
		storePreviewContent: ByteArray? = null,

		block: ChartSetBuilder.() -> Unit
	) = ChartSetBuilder(
		this,
		setTitle,
		composerName,
		noterUser,
		coinPrice,
		introduction,
		needReview,
		expectStatus,
		defaultId,
		musicContent,
		previewMusicContent,
		setCoverContent,
		storePreviewContent
	).apply(block).buildSet()

	/**
	 * Use [IBlowAccessor.buildChartSet][buildChartSet] instead.
	 */
	class ChartSetBuilder internal constructor(
		private val accessor: IBlowAccessor,

		private val musicName: String,
		private val composerName: String,
		private val noterUser: MongoUser?,
		private val price: Int,
		private val introduction: String = "",
		private val needReview: Boolean = true,
		private val status: SetStatus = SetStatus.UNRANKED,
		private val defaultId: String? = null,

		private val musicContent: ByteArray? = null,
		private val previewMusicContent: ByteArray? = null,
		private val setCoverContent: ByteArray? = null,
		private val storePreviewContent: ByteArray? = null
	) {

		private val charts = mutableMapOf<Int, MongoChart>()

		fun addChart(
			difficultyClass: Int,
			difficultyValue: Int,
			D: Double? = null,
			defaultId: String? = null,
			content: ByteArray? = null
		): MongoChart {
			// Unranked charts should have no D value
			val d = if(status.isRanked) D else null

			// Check difficulty duplication
			if(difficultyClass in charts.keys) {
				error("Duplicated Difficulty Class: $difficultyClass")
			}

			return accessor.createChart(difficultyClass, difficultyValue, defaultId, d, content).apply {
				charts[difficultyClass] = this
			}
		}

		fun buildSet(): MongoSet {
			if(charts.isEmpty()) error("Must include at least one chart.")
			return accessor.createSet(
				musicName,
				composerName,
				noterId = noterUser?.id,
				charts = charts.values.toList(),
				defaultId,
				introduction,
				status,
				price,
				displayNoterName = null,
				musicContent,
				previewMusicContent,
				setCoverContent,
				storePreviewContent
			).applyIf(needReview) {
				with(accessor) { startReview() }
			}
		}
	}

	fun MongoUser.buySet(id: String): ExchangeSetModel

	fun MongoUser.submitBeforeAssessment(assessmentId: String, medal: Int): BeforePlaySubmitModel

	fun MongoUser.submitBeforePlay(chartId: String, ppCost: Int, eventArgs: String): BeforePlaySubmitModel

	fun MongoUser.submitAfterAssessment(records: List<PlayRecordInput>, randomId: String): AfterAssessmentModel

	fun MongoUser.submitAfterPlay(record: PlayRecordInput, randomId: String): AfterPlaySubmitModel

	fun MongoSet.getReview(): MongoReview?

	fun MongoSet.startReview()

	@Deprecated("ExpectStatus is no longer required. Status will not be NEED_REVIEW any more.")
	fun MongoSet.startReview(expectStatus: SetStatus)

	fun MongoReview.addReview(userId: String, status: Boolean, evaluation: String)

	/**
	 * End a review on the specific Set.
	 */
	fun MongoSet.endReview(pass: Boolean)

	fun MongoUser.canAffordCoin(coin: Int): Boolean {
		return this.coin >= coin
	}

	fun MongoUser.canAffordDiamond(diamond: Int): Boolean {
		return this.diamond >= diamond
	}

	fun MongoUser.payCoin(coin: Int): Boolean
	fun MongoUser.payDiamond(diamond: Int): Boolean
}