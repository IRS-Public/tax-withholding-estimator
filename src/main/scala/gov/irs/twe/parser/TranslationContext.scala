package gov.irs.twe.parser

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import scala.collection.mutable

case class TranslationContext(
    translationMap: mutable.LinkedHashMap[String, Any] = mutable.LinkedHashMap.empty,
    translationContext: List[String] = List.empty,
    tagCounts: mutable.Map[String, Int] = mutable.Map.empty,
) {
  // This should be used minimally. If we have lots of duplicate content, something is awry
  def forChildWithoutUniqueId(label: String): TranslationContext = {
    val childKey = nextChildKey(label)
    val currentMap = translationMap.getMap(translationContext)
    currentMap.getOrElseUpdate(childKey, mutable.LinkedHashMap.empty[String, Any])
    TranslationContext(translationMap, translationContext :+ childKey)
  }

  def forChildWithoutUniqueId(label: String, uniqueContent: String): TranslationContext = {
    val childKey = getHashKey(label, uniqueContent)
    val currentMap = translationMap.getMap(translationContext)
    currentMap.getOrElseUpdate(childKey, mutable.LinkedHashMap.empty[String, Any])
    TranslationContext(translationMap, translationContext :+ childKey)
  }

  def forChildWithId(id: String): TranslationContext = {
    val currentMap = translationMap.getMap(translationContext)
    currentMap.getOrElseUpdate(id, mutable.LinkedHashMap.empty[String, Any])
    TranslationContext(translationMap, translationContext :+ id)
  }

  def nextChildKey(label: String) = {
    val count = (tagCounts.getOrElse(label, -1)) + 1
    tagCounts(label) = count
    s"$label-$count"
  }

  def getHashKey(label: String, content: String): String = {
    val digest = MessageDigest.getInstance("MD5")
    val hexString = digest.digest(content.getBytes(StandardCharsets.UTF_8)).map("%02x".format(_)).mkString
    // We can use the entire hexKey if are okay with longer keys and want to avoid unintentional duplicate keys
    s"$label-${hexString.take(6)}"
  }

  def updateValue(key: String, value: String): Unit = {
    val currentMap = translationMap.getMap(translationContext)
    if (currentMap.contains(key)) {
      val existingContent = currentMap.get(key)
      val contentString = existingContent match {
        case Some(value) => value.toString
        case None        => throw new Exception("Expected a string value")
      }
      if (contentString != value) {
        throw IllegalArgumentException(
          s"Collision detected. Expected unique translation key: \"$key\", but an entry with a matching key and differing content already existed. To resolve this error you may want to increase the number of characters returned by getHashKey.",
        )
      }
    }
    currentMap += key -> value
  }

  def fullKey(): String = {
    translationContext.mkString(".")
  }

  def fullKey(localKey: String): String = {
    if (translationContext.isEmpty) localKey else s"${translationContext.mkString(".")}.$localKey"
  }
}

extension (translationMap: mutable.LinkedHashMap[String, Any]) {
  def getMap(keys: List[String]): mutable.LinkedHashMap[String, Any] = {
    val output = keys.foldLeft(Option(translationMap: Any)) {
      case (Some(m: mutable.LinkedHashMap[String, Any] @unchecked), key) => m.get(key)
      case _ => throw new IllegalArgumentException("invalid key path to translation map")
    }
    output.get match
      case m: mutable.LinkedHashMap[String, Any] @unchecked => m
      case _                                                =>
        throw new IllegalArgumentException(
          s"expected value to be of type mutable.LinkedHashMap[String, Any], but was ${output.get.getClass.getName}",
        )
  }
}
