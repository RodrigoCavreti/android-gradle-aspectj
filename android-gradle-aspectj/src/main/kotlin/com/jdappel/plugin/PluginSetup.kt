package com.jdappel.plugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.jdappel.AndroidConfig
import com.jdappel.AspectJExtension
import com.jdappel.MISDEFINITION
import com.jdappel.RETROLAMBDA
import com.jdappel.api.AspectJCompileTask
import com.jdappel.api.BuildTimeListener
import com.jdappel.utils.getJavaTask
import com.jdappel.utils.getVariantDataList
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer

internal fun configProject(project: Project, config: AndroidConfig, settings: AspectJExtension) {
    if (settings.extendClasspath) {
        project.repositories.mavenCentral()
        project.dependencies.add("implementation", "org.aspectj:aspectjrt:${settings.ajc}")
    }

    project.afterEvaluate {
        prepareVariant(config)
        configureCompiler(project, config)
    }

    if (settings.buildTimeLog) {
        project.gradle.addListener(BuildTimeListener())
    }

    checkIfPluginAppliedAfterRetrolambda(project)
}

private fun prepareVariant(config: AndroidConfig) {
    val sets = config.extAndroid.sourceSets

    fun applier(path: String) = sets.getByName(path).java.srcDir("src/$path/aspectj")

    // general sets
    arrayOf("main", "test", "androidTest").forEach {
        sets.getByName(it).java.srcDir("src/$it/aspectj")
    }

    // applies srcSet 'aspectj' for each build variant
    getVariantDataList(config.plugin).forEach { variant ->
        variant.variantConfiguration.productFlavors.forEach { applier(it.name) }
        applier(variant.variantConfiguration.buildType.name)
    }
}

private fun configureCompiler(project: Project, config: AndroidConfig) {
    getVariantDataList(config.plugin).forEach variantScanner@ { variant ->
        val variantName = variant.name.capitalize()

        // do not configure compiler task for non-test variants in ConfigScope.TEST
        if (config.scope == ConfigScope.TEST && !variantName.contains("androidtest", true))
            return@variantScanner

        val taskName = "compile${variantName}AspectJ"
        AspectJCompileTask.Builder(project)
            .plugin(project.plugins.getPlugin(config))
            .config(project.extensions.getByType(AspectJExtension::class.java))
            .compiler(getJavaTask(variant)!!)
            .variant(variant.name)
            .name(taskName)
            .buildAndAttach(config)
    }
}

private fun checkIfPluginAppliedAfterRetrolambda(project: Project) {
    val appears = project.plugins.hasPlugin(RETROLAMBDA)
    if (!appears) {
        project.afterEvaluate {
            //RL was defined before AJ plugin
            if (!appears && project.plugins.hasPlugin(RETROLAMBDA)) {
                throw GradleException(MISDEFINITION)
            }
        }
    }
}

private inline fun <reified T> PluginContainer.getPlugin(config: AndroidConfig): T where T : Plugin<Project> {
    @Suppress("UNCHECKED_CAST")
    val plugin: Class<out T> = (if (config.isLibraryPlugin) LibraryPlugin::class.java else AppPlugin::class.java) as Class<T>
    return getPlugin(plugin)
}