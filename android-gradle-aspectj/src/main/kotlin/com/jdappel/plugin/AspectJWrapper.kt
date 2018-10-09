package com.jdappel.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.jdappel.AndroidConfig
import com.jdappel.AspectJExtension
import com.jdappel.api.transform.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import javax.inject.Inject

internal sealed class AspectJWrapper(private val scope: ConfigScope): Plugin<Project> {

    internal class Standard @Inject constructor(): AspectJWrapper(ConfigScope.STANDARD) {
        override fun getTransformer(project: Project): AspectJTransform = StandardTransformer(project)
    }

    internal class Provides @Inject constructor(): AspectJWrapper(ConfigScope.PROVIDE) {
        override fun getTransformer(project: Project): AspectJTransform = ProvidesTransformer(project)
    }

    internal class Extended @Inject constructor(): AspectJWrapper(ConfigScope.EXTEND) {
        override fun getTransformer(project: Project): AspectJTransform = ExtendedTransformer(project)
    }

    internal class Test @Inject constructor(): AspectJWrapper(ConfigScope.TEST) {
        override fun getTransformer(project: Project): AspectJTransform = TestsTransformer(project)
    }

    override fun apply(project: Project) {
        val config = AndroidConfig(project, scope)
        val settings = project.extensions.create("aspectj", AspectJExtension::class.java)

        configProject(project, config, settings)

        val module: TestedExtension
        val transformer: AspectJTransform
        if (config.isLibraryPlugin) {
            transformer = LibraryTransformer(project)
            module = project.extensions.getByType(LibraryExtension::class.java)
        } else {
            transformer = getTransformer(project)
            module = project.extensions.getByType(AppExtension::class.java)
        }

        transformer.withConfig(config).prepareProject()
        module.registerTransform(transformer)
    }

    internal abstract fun getTransformer(project: Project): AspectJTransform
}