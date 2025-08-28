package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact

enum FgCollectionNode {
  case fgSet(fact: FgSet)
  case rawHTML(node: xml.Node)
}

case class FgCollection(path: String, condition: Option[Condition], nodes: List[FgCollectionNode]) {
  def html(): xml.Elem = {
    val collectionFacts = this.nodes.map {
      case FgCollectionNode.fgSet(x)   => x.html()
      case FgCollectionNode.rawHTML(x) => x
    }

    val condition = this.condition.map(_.path).orNull
    val operator = this.condition.map(_.operator.toString).orNull

    // TODO: integrate localization instead of inlining text
    // TODO: Change return type https://github.com/IRSDigitalService/tax_withholding_estimator/issues/195
    <fg-collection path={this.path} condition={condition} operator={operator}>
      <template class="fg-collection__item-template">
        <fieldset class="usa-fieldset margin-top-3 padding-3 border-1px border-base-lighter">
          <div class="fg-collection-item__controls">
            <button type="button" class="fg-collection-item__remove-item usa-button usa-button--unstyled">Remove item</button>
          </div>
          <div class="fg-collection-item__fields">
            {collectionFacts}
          </div>
        </fieldset>
      </template>
      <button type="button" class="fg-collection__add-item usa-button usa-button--outline">
      <svg aria-hidden="true" role="img" focusable="false" class="usa-icon usa-icon--size-3 margin-left-neg-1 margin-y-neg-05">
          <use href="/resources/uswds-3.13.0/img/sprite.svg#add"></use>
        </svg>Add item</button>
    </fg-collection>
  }
}

object FgCollection {
  def parse(node: xml.Node, factDictionary: FactDictionary): FgCollection = {
    val path = node \@ "path"
    val condition = Condition.getCondition(node, factDictionary)

    validateFgCollection(path, factDictionary)

    val nodes = (node \ "_")
      .map(node =>
        node.label match {
          case "fg-set" => FgCollectionNode.fgSet(FgSet.parse(node, factDictionary))
          case _        => FgCollectionNode.rawHTML(node)
        },
      )
      .toList

    FgCollection(path, condition, nodes)
  }

  private def validateFgCollection(path: String, factDictionary: FactDictionary): Unit = {
    validateFact(path, factDictionary)
    if (factDictionary.getDefinition(path).typeNode != "CollectionNode")
      throw InvalidFormConfig(s"Path $path must be of type CollectionNode")
  }
}
