package gov.irs.twe.parser

import scala.collection.mutable

case class TranslationContext(
    translationMap: mutable.LinkedHashMap[String, Any] = mutable.LinkedHashMap.empty,
    translationContext: List[String] = List.empty,
    tagCounts: mutable.Map[String, Int] = mutable.Map.empty,
) {
  def forChildWithoutUniqueId(label: String): TranslationContext = {
    val childKey = nextChildKey(label)
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

  def updateValue(key: String, value: String): Unit = {
    val currentMap = translationMap.getMap(translationContext)
    if (currentMap.contains(key)) {
      throw IllegalArgumentException(
        s"Expected unique translation key: \"$key\", but an entry with a matching key already existed",
      )
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
