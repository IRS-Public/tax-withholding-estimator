package gov.irs.twe.templates

import org.scalatest.funspec.AnyFunSpec

import scala.io.Source
import scala.util.Using

class TemplateSpec extends AnyFunSpec {

  describe("Thymeleaf templates") {

    val templateDir = "twe/templates"

    val dir = {
      val classLoader = getClass.getClassLoader
      val url = Option(classLoader.getResource(templateDir))
        .getOrElse(fail(s"Template directory not found on classpath: $templateDir"))
      new java.io.File(url.toURI)
    }

    // Recursively find all .html template files
    val templatePaths = {
      def walk(f: java.io.File): List[java.io.File] = {
        if (f.isDirectory) f.listFiles().toList.flatMap(walk)
        else if (f.getName.endsWith(".html")) List(f)
        else Nil
      }
      walk(dir)
    }

    // Regex matches th:replace="..." or th:insert="..." where the value is NOT wrapped in ~{...}
    // and is NOT a ${...} dynamic expression (which is valid Thymeleaf syntax)
    val deprecatedFragmentPattern =
      """th:(replace|insert)="(?!\~\{)(?!\$\{)([^"]+)"""".r

    it("should not use deprecated unwrapped fragment expressions") {
      val violations = templatePaths.flatMap { file =>
        val relativePath = dir.toPath.relativize(file.toPath).toString
        Using.resource(Source.fromFile(file)) { source =>
          source.getLines().zipWithIndex.toList.flatMap { case (line, lineNum) =>
            deprecatedFragmentPattern.findAllMatchIn(line).map { m =>
              s"  $relativePath:${lineNum + 1} — th:${m.group(1)}=\"${m.group(2)}\" should be th:${m.group(1)}=\"~{${m.group(2)}}\""
            }
          }
        }
      }

      assert(
        violations.isEmpty,
        s"\nFound ${violations.size} deprecated Thymeleaf fragment expression(s):\n${violations.mkString("\n")}",
      )
    }
  }

}
