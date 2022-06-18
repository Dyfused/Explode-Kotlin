package explode.schema

import explode.schema.model.*

val FakeChart = ChartModel(
	"10001",
	1,
	1
)

val FakeSet = SetModel(
	"10001",
	"",
	0,
	NoterModel("FakeUser-TheNoter"),
	"FakeMusic-TheTitle",
	"FakeName-Composer",
	0,
	listOf(FakeChart),
	true,
	false,
	"unknown"
)

val FakeAssessmentChart = AssessmentChartModel(
	"10001",
	FakeSet
)

val FakeAssessment = AssessmentModel(
	"10001",
	1,
	0.5,
	0.6,
	0.8,
	0.9,
	listOf(FakeAssessmentChart),
	listOf(AssessmentRecordModel(0.5, true, listOf(AssessmentPlayRecordModel(1000, 20, -10, 1000000))))
)

val FakeAssessment2 = AssessmentModel(
	"10002",
	1,
	0.5,
	0.6,
	0.8,
	0.9,
	listOf(FakeAssessmentChart),
	listOf(AssessmentRecordModel(100.0, true, listOf(AssessmentPlayRecordModel(1000, 20, -10, 1000000))))
)

val FakeAssessmentGroup = AssessmentGroupModel("10001", "FakeAssessmentGroup-TheName", listOf(FakeAssessment))
val FakeAssessmentGroup2 = AssessmentGroupModel("10002", "FakeAssessmentGroup2-TheName", listOf(FakeAssessment2))