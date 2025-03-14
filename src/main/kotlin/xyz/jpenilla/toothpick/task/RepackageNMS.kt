/*
 * This file is part of Toothpick, licensed under the MIT License.
 *
 * Copyright (c) 2020-2021 Jason Penilla & Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package xyz.jpenilla.toothpick.task

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL

public open class RepackageNMS : ToothpickTask() {
  public companion object {
    /**
     * [The default URL to download class mappings from.](https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/mappings/bukkit-1.16.5-cl.csrg?at=80d35549ec67b87a0cdf0d897abbe826ba34ac27)
     */
    public val DEFAULT_CLASS_MAPPINGS_URL: URL = URL("https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/mappings/bukkit-1.16.5-cl.csrg?at=80d35549ec67b87a0cdf0d897abbe826ba34ac27")
  }

  /**
   * The URL used to download class mappings from when repackaging patches.
   */
  @Input
  public var classMappingsUrl: URL = DEFAULT_CLASS_MAPPINGS_URL

  @TaskAction
  public fun repackage() {
    logger.lifecycle("Downloading class mappings...")
    val mappingsFileText = classMappingsUrl.readText()
    logger.lifecycle("Done downloading class mappings.")

    logger.lifecycle(">>> Preparing patches for NMS repackage")
    val mappings = mappingsFileText
      .split("\n")
      .asSequence()
      .map { it.trim() }
      .filter { !it.startsWith("#") && !it.contains("$") && it.isNotEmpty() }
      .map { it.split(" ")[1].replace("/", ".") }
      .map { Mapping(it) }
      .filter { it.newFQName != it.oldFQName }
      .toSet()
    val remapper = Remapper(mappings)

    toothpick.subprojects
      .map { it.patchesDir }
      .forEach { patchesDir ->
        val repackagedPatchesDir =
          patchesDir.parentFile.resolve("${patchesDir.name}_repackaged-${System.currentTimeMillis()}")
        repackagedPatchesDir.mkdir()
        val patchFiles = patchesDir.listFiles()?.toList() ?: error("Could not list patch files")
        patchFiles.parallelStream().forEach { patch ->
          logger.lifecycle("Processing ${patchesDir.name}/${patch.name}...")
          val newPatch = repackagedPatchesDir.resolve(patch.name)
          newPatch.writeText(remapper.remapFile(patch))
        }
      }

    logger.lifecycle(">>> Done preparing patches")
  }

  private class Mapping(fullyQualifiedClassName: String) {
    val className = fullyQualifiedClassName.substringAfterLast(".")
    val oldFQName = "net.minecraft.server.$className"
    val oldJavaFileName = "net/minecraft/server/$className.java"
    val newFQName = fullyQualifiedClassName
    val newJavaFileName = "${fullyQualifiedClassName.replace(".", "/")}.java"
  }

  private class Remapper(private val mappings: Set<Mapping>) {
    fun remapFile(file: File): String =
      file.readLines().joinToString("\n") { remapLine(it) } + "\n"

    private fun remapLine(line: String): String {
      if (
        line.startsWith("diff --git ")
        || line.startsWith("+++ ")
        || line.startsWith("--- ")
      ) {
        var text = line
        mappings.forEach { text = text.replace(it.oldJavaFileName, it.newJavaFileName) }
        return text
      }
      if (line.startsWith("+")) {
        var text = line
        mappings.forEach { text = text.replace(it.oldFQName, it.newFQName) }
        return text
      }
      return line
    }
  }
}
