package com.jdappel.api.transform

import org.gradle.api.Project

/**
 * Simple transformers declarations and common data
 *
 * @author archinamon on 07.10.17.
 */

internal const val TRANSFORM_NAME = "aspectj"
const val AJRUNTIME = "aspectjrt"
const val SLICER_DETECTED_ERROR = "Running with InstantRun slicer when weaver extended not allowed!"

enum class BuildPolicy {

    SIMPLE,
    COMPLEX,
    LIBRARY
}

internal class StandardTransformer(project: Project): AspectJTransform(project, BuildPolicy.SIMPLE)
internal class ExtendedTransformer(project: Project): AspectJTransform(project, BuildPolicy.COMPLEX)
internal class TestsTransformer(project: Project): AspectJTransform(project, BuildPolicy.COMPLEX)