package com.example.model

import android.graphics.RectF

/**
 * Factory providing concrete SceneAnalysis payloads for all 15 test scenarios.
 */
object SimulationScenariosProvider {

    fun getAnalysisForScenario(scenario: SimulationScenario): SceneAnalysis {
        return when (scenario) {
            SimulationScenario.DAYLIGHT_CLEAR -> SceneAnalysis(
                scene = SceneType.DAYLIGHT,
                confidence = 0.94f,
                lighting = LightingAnalysis(brightness = 68.0f, darkness = 32.0f, contrast = 52.0f, highlightClipping = 1.5f, shadowLevel = 1.2f, condition = LightingCondition.NORMAL),
                subject = SubjectAnalysis.DEFAULT,
                motion = MotionAnalysis(motionScore = 0.04f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 95, exposureScore = 96, sharpnessScore = 94, stabilityScore = 96, dynamicRangeScore = 94, ratingLabel = "EXCELLENT"),
                sharpnessMetric = 70.0f,
                estimatedKelvin = 5500
            )

            SimulationScenario.INDOOR_OFFICE -> SceneAnalysis(
                scene = SceneType.INDOOR,
                confidence = 0.88f,
                lighting = LightingAnalysis(brightness = 48.0f, darkness = 52.0f, contrast = 46.0f, highlightClipping = 2.0f, shadowLevel = 4.0f, condition = LightingCondition.NORMAL),
                subject = SubjectAnalysis(numberOfFaces = 1, isPersonPresent = true, approximateSubjectSize = "Medium", isLikelyPortrait = false),
                motion = MotionAnalysis(motionScore = 0.05f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 88, exposureScore = 90, sharpnessScore = 86, stabilityScore = 92, dynamicRangeScore = 88, ratingLabel = "GREAT"),
                sharpnessMetric = 54.0f,
                estimatedKelvin = 4200
            )

            SimulationScenario.HARSH_BACKLIGHT -> SceneAnalysis(
                scene = SceneType.PORTRAIT,
                confidence = 0.91f,
                lighting = LightingAnalysis(brightness = 75.0f, darkness = 25.0f, contrast = 88.0f, highlightClipping = 16.0f, shadowLevel = 6.0f, condition = LightingCondition.BRIGHT),
                subject = SubjectAnalysis(
                    numberOfFaces = 1,
                    isPersonPresent = true,
                    approximateSubjectSize = "Large",
                    isLikelyPortrait = true,
                    skinRatio = 0.16f,
                    detectedFaces = listOf(
                        DetectedFace(
                            bounds = RectF(0.35f, 0.20f, 0.65f, 0.60f),
                            faceBrightness = 32.0f,
                            faceExposureRelativeToScene = -43.0f,
                            faceClipping = 0.0f,
                            faceShadowLevel = 18.0f
                        )
                    ),
                    primaryFaceBrightness = 32.0f,
                    primaryFaceExposureRelativeToScene = -43.0f,
                    primaryFaceShadowLevel = 18.0f
                ),
                motion = MotionAnalysis(motionScore = 0.05f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 82, exposureScore = 80, sharpnessScore = 88, stabilityScore = 92, dynamicRangeScore = 78, ratingLabel = "GREAT"),
                sharpnessMetric = 60.0f,
                skyDetected = true,
                estimatedKelvin = 5800
            )

            SimulationScenario.BLOWN_SKY_LANDSCAPE -> SceneAnalysis(
                scene = SceneType.FOREST_NATURE,
                confidence = 0.92f,
                lighting = LightingAnalysis(brightness = 72.0f, darkness = 28.0f, contrast = 78.0f, highlightClipping = 18.5f, shadowLevel = 8.0f, condition = LightingCondition.BRIGHT),
                subject = SubjectAnalysis.DEFAULT,
                motion = MotionAnalysis(motionScore = 0.03f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 84, exposureScore = 82, sharpnessScore = 92, stabilityScore = 95, dynamicRangeScore = 79, ratingLabel = "GREAT"),
                sharpnessMetric = 74.0f,
                greenVegetationRatio = 0.42f,
                skyDetected = true,
                estimatedKelvin = 6200
            )

            SimulationScenario.SUNSET_DYNAMIC_RANGE -> SceneAnalysis(
                scene = SceneType.SUNSET,
                confidence = 0.96f,
                lighting = LightingAnalysis(brightness = 42.0f, darkness = 58.0f, contrast = 82.0f, highlightClipping = 12.0f, shadowLevel = 22.0f, condition = LightingCondition.NORMAL),
                subject = SubjectAnalysis.DEFAULT,
                motion = MotionAnalysis(motionScore = 0.03f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 91, exposureScore = 90, sharpnessScore = 88, stabilityScore = 96, dynamicRangeScore = 86, ratingLabel = "EXCELLENT"),
                sharpnessMetric = 52.0f,
                warmColorRatio = 0.52f,
                skyDetected = true,
                estimatedKelvin = 3100
            )

            SimulationScenario.BEACH_HIGH_ALBEDO -> SceneAnalysis(
                scene = SceneType.BEACH,
                confidence = 0.93f,
                lighting = LightingAnalysis(brightness = 86.0f, darkness = 14.0f, contrast = 68.0f, highlightClipping = 15.0f, shadowLevel = 1.0f, condition = LightingCondition.VERY_BRIGHT),
                subject = SubjectAnalysis.DEFAULT,
                motion = MotionAnalysis(motionScore = 0.08f, motionLevel = MotionLevel.LOW, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 89, exposureScore = 88, sharpnessScore = 87, stabilityScore = 92, dynamicRangeScore = 85, ratingLabel = "GREAT"),
                sharpnessMetric = 58.0f,
                coolBlueRatio = 0.44f,
                skyDetected = true,
                estimatedKelvin = 5700
            )

            SimulationScenario.LOW_LIGHT_INDOOR -> SceneAnalysis(
                scene = SceneType.LOW_LIGHT,
                confidence = 0.90f,
                lighting = LightingAnalysis(brightness = 26.0f, darkness = 74.0f, contrast = 48.0f, highlightClipping = 1.5f, shadowLevel = 26.0f, condition = LightingCondition.DARK),
                subject = SubjectAnalysis.DEFAULT,
                motion = MotionAnalysis(motionScore = 0.06f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 81, exposureScore = 79, sharpnessScore = 80, stabilityScore = 91, dynamicRangeScore = 80, ratingLabel = "GREAT"),
                sharpnessMetric = 44.0f,
                estimatedKelvin = 4200
            )

            SimulationScenario.EXTREME_NIGHT_STATIC -> SceneAnalysis(
                scene = SceneType.NIGHT,
                confidence = 0.97f,
                lighting = LightingAnalysis(brightness = 8.0f, darkness = 92.0f, contrast = 72.0f, highlightClipping = 4.0f, shadowLevel = 42.0f, condition = LightingCondition.VERY_DARK),
                subject = SubjectAnalysis.DEFAULT,
                motion = MotionAnalysis(motionScore = 0.02f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 79, exposureScore = 72, sharpnessScore = 76, stabilityScore = 95, dynamicRangeScore = 74, ratingLabel = "GOOD"),
                sharpnessMetric = 40.0f,
                estimatedKelvin = 3800
            )

            SimulationScenario.HIGH_CONTRAST_NIGHT_WINDOW -> SceneAnalysis(
                scene = SceneType.NIGHT,
                confidence = 0.91f,
                lighting = LightingAnalysis(brightness = 18.0f, darkness = 82.0f, contrast = 92.0f, highlightClipping = 14.0f, shadowLevel = 38.0f, condition = LightingCondition.DARK),
                subject = SubjectAnalysis.DEFAULT,
                motion = MotionAnalysis(motionScore = 0.05f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 78, exposureScore = 74, sharpnessScore = 77, stabilityScore = 90, dynamicRangeScore = 73, ratingLabel = "GOOD"),
                sharpnessMetric = 45.0f,
                estimatedKelvin = 4600
            )

            SimulationScenario.PORTRAIT_SOFT_LIGHT -> SceneAnalysis(
                scene = SceneType.PORTRAIT,
                confidence = 0.95f,
                lighting = LightingAnalysis(brightness = 55.0f, darkness = 45.0f, contrast = 42.0f, highlightClipping = 1.0f, shadowLevel = 1.5f, condition = LightingCondition.NORMAL),
                subject = SubjectAnalysis(
                    numberOfFaces = 1,
                    isPersonPresent = true,
                    approximateSubjectSize = "Large",
                    isLikelyPortrait = true,
                    skinRatio = 0.22f,
                    detectedFaces = listOf(
                        DetectedFace(
                            bounds = RectF(0.30f, 0.20f, 0.70f, 0.65f),
                            faceBrightness = 54.0f,
                            faceExposureRelativeToScene = -1.0f,
                            faceClipping = 0.0f,
                            faceShadowLevel = 1.0f
                        )
                    ),
                    primaryFaceBrightness = 54.0f,
                    primaryFaceExposureRelativeToScene = -1.0f
                ),
                motion = MotionAnalysis(motionScore = 0.03f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 96, exposureScore = 97, sharpnessScore = 94, stabilityScore = 97, dynamicRangeScore = 95, ratingLabel = "EXCELLENT"),
                sharpnessMetric = 66.0f,
                estimatedKelvin = 5200
            )

            SimulationScenario.BACKLIT_PORTRAIT_FACE -> SceneAnalysis(
                scene = SceneType.PORTRAIT,
                confidence = 0.93f,
                lighting = LightingAnalysis(brightness = 70.0f, darkness = 30.0f, contrast = 84.0f, highlightClipping = 14.0f, shadowLevel = 8.0f, condition = LightingCondition.BRIGHT),
                subject = SubjectAnalysis(
                    numberOfFaces = 1,
                    isPersonPresent = true,
                    approximateSubjectSize = "Large",
                    isLikelyPortrait = true,
                    skinRatio = 0.18f,
                    detectedFaces = listOf(
                        DetectedFace(
                            bounds = RectF(0.35f, 0.25f, 0.65f, 0.65f),
                            faceBrightness = 28.0f,
                            faceExposureRelativeToScene = -42.0f,
                            faceClipping = 0.0f,
                            faceShadowLevel = 22.0f
                        )
                    ),
                    primaryFaceBrightness = 28.0f,
                    primaryFaceExposureRelativeToScene = -42.0f,
                    primaryFaceShadowLevel = 22.0f
                ),
                motion = MotionAnalysis(motionScore = 0.04f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 81, exposureScore = 78, sharpnessScore = 86, stabilityScore = 92, dynamicRangeScore = 78, ratingLabel = "GREAT"),
                sharpnessMetric = 58.0f,
                estimatedKelvin = 5600
            )

            SimulationScenario.GROUP_PORTRAIT -> SceneAnalysis(
                scene = SceneType.PORTRAIT,
                confidence = 0.92f,
                lighting = LightingAnalysis(brightness = 58.0f, darkness = 42.0f, contrast = 50.0f, highlightClipping = 2.0f, shadowLevel = 2.5f, condition = LightingCondition.NORMAL),
                subject = SubjectAnalysis(
                    numberOfFaces = 3,
                    isPersonPresent = true,
                    approximateSubjectSize = "Medium",
                    isLikelyPortrait = true,
                    skinRatio = 0.15f,
                    detectedFaces = listOf(
                        DetectedFace(bounds = RectF(0.15f, 0.30f, 0.35f, 0.60f), faceBrightness = 56f),
                        DetectedFace(bounds = RectF(0.40f, 0.25f, 0.60f, 0.60f), faceBrightness = 58f),
                        DetectedFace(bounds = RectF(0.65f, 0.30f, 0.85f, 0.60f), faceBrightness = 55f)
                    ),
                    primaryFaceBrightness = 58.0f
                ),
                motion = MotionAnalysis(motionScore = 0.06f, motionLevel = MotionLevel.LOW, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 92, exposureScore = 93, sharpnessScore = 90, stabilityScore = 93, dynamicRangeScore = 91, ratingLabel = "EXCELLENT"),
                sharpnessMetric = 64.0f,
                estimatedKelvin = 5000
            )

            SimulationScenario.RUNNING_CHILD_HIGH_MOTION -> SceneAnalysis(
                scene = SceneType.DAYLIGHT,
                confidence = 0.90f,
                lighting = LightingAnalysis(brightness = 64.0f, darkness = 36.0f, contrast = 55.0f, highlightClipping = 2.0f, shadowLevel = 2.0f, condition = LightingCondition.NORMAL),
                subject = SubjectAnalysis(numberOfFaces = 1, isPersonPresent = true, approximateSubjectSize = "Medium"),
                motion = MotionAnalysis(motionScore = 0.58f, motionLevel = MotionLevel.HIGH, isBlurRisk = true),
                photoQuality = PhotoQualityScore(totalScore = 84, exposureScore = 92, sharpnessScore = 80, stabilityScore = 65, dynamicRangeScore = 90, ratingLabel = "GREAT"),
                sharpnessMetric = 50.0f,
                estimatedKelvin = 5500
            )

            SimulationScenario.HANDHELD_JITTER_LOW_LIGHT -> SceneAnalysis(
                scene = SceneType.LOW_LIGHT,
                confidence = 0.87f,
                lighting = LightingAnalysis(brightness = 22.0f, darkness = 78.0f, contrast = 45.0f, highlightClipping = 1.0f, shadowLevel = 28.0f, condition = LightingCondition.DARK),
                subject = SubjectAnalysis.DEFAULT,
                motion = MotionAnalysis(motionScore = 0.32f, motionLevel = MotionLevel.MODERATE, isBlurRisk = true),
                photoQuality = PhotoQualityScore(totalScore = 72, exposureScore = 74, sharpnessScore = 68, stabilityScore = 70, dynamicRangeScore = 75, ratingLabel = "FAIR"),
                sharpnessMetric = 36.0f,
                estimatedKelvin = 4100
            )

            SimulationScenario.WARM_CANDLELIGHT_INDOOR -> SceneAnalysis(
                scene = SceneType.INDOOR,
                confidence = 0.91f,
                lighting = LightingAnalysis(brightness = 30.0f, darkness = 70.0f, contrast = 64.0f, highlightClipping = 4.0f, shadowLevel = 18.0f, condition = LightingCondition.DARK),
                subject = SubjectAnalysis(numberOfFaces = 1, isPersonPresent = true, approximateSubjectSize = "Medium"),
                motion = MotionAnalysis(motionScore = 0.04f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
                photoQuality = PhotoQualityScore(totalScore = 85, exposureScore = 84, sharpnessScore = 82, stabilityScore = 93, dynamicRangeScore = 83, ratingLabel = "GREAT"),
                sharpnessMetric = 46.0f,
                warmColorRatio = 0.65f,
                estimatedKelvin = 2200
            )
        }
    }
}
