const parser = new DOMParser()

const res = await fetch('/twe/resources/fact-dictionary.xml')
const text = await res.text()
const factDictionaryXml = parser.parseFromString(text, "application/xml")

const auditedFactsList = document.querySelector('#audit-panel__fact-list')
const pathSelect = document.querySelector('#fact-select')
const pathOptions = document.querySelector('#fact-options')
const addFactButton = document.querySelector('#add-fact-button')

addFactButton.addEventListener('click', trackSelectedFact)
pathSelect.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') trackSelectedFact()
})

if (document.readyState !== 'complete') {
  document.addEventListener('DOMContentLoaded', loadPaths)
} else {
  loadPaths()
}

function makeCollectionIdPath(abstractPath, id) {
  return abstractPath.replace('*', `#${id}`);
}

function trackSelectedFact() {
  const factPath = pathSelect.value;
  const collectionId = document.querySelector('#fact-collection-id').value
  if (factPath) {
    trackFact(factPath, collectionId)
  }
}

function trackFact(path, collectionId) {
  const auditedFact = document.createElement('audited-fact')

  const factPath = makeCollectionIdPath(path, collectionId)
  if (AuditedFact.selectedFacts.has(factPath)) {
    console.debug(`Already tracking ${factPath}`)
    return
  } else {
    console.debug(`Tracking ${factPath}`)
  }

  auditedFact.setAttribute('path', path)
  auditedFact.setAttribute('collectionId', collectionId)

  const listItem = document.createElement('li')
  listItem.appendChild(auditedFact)
  auditedFactsList.appendChild(listItem)
}

function loadPaths() {
  const paths = factGraph.paths().sort()

  pathOptions.append(...paths.map((path) => {
    const option = document.createElement('option')
    option.text = option.value = path
    return option
  }))
}

class AuditedFact extends HTMLElement {
  static selectedFacts = new Set()
  static xmlSerializer = new XMLSerializer()
  static dependencyClassName= 'audit-panel__fact__definition__dependency'

  constructor() {
    super()

    this.deleteListener = () => this.parentElement.remove()
    this.renderListener = () => this.render()
    this.trackDependencyListener = (e) => {
      trackFact(e.target.href.replace(/.*#\//, '/'), this.getAttribute('collectionid'))

      e.preventDefault()

      return false
    }

    const templateContent = document.querySelector('#audit-panel__fact').content.cloneNode(true)
    this.attachShadow({mode: 'open'})
    this.shadowRoot.append(templateContent)

    this.factPathElem = this.shadowRoot.querySelector('.audit-panel__fact__path')
    this.factTypeElem = this.shadowRoot.querySelector('.audit-panel__fact__type')
    this.factValueElem = this.shadowRoot.querySelector('.audit-panel__fact__value')
    this.factDefinitionElem = this.shadowRoot.querySelector(`.audit-panel__fact__definition`)

    this.removeButton = this.shadowRoot.querySelector('.audit-panel__fact__remove')
  }

  connectedCallback() {
    this.abstractPath = this.getAttribute('path')
    this.collectionId = this.getAttribute('collectionid')

    this.factPath = makeCollectionIdPath(this.abstractPath, this.collectionId)
    this.removeButton.addEventListener('click', this.deleteListener)
    document.addEventListener(`fg-update`, this.renderListener)

    AuditedFact.selectedFacts.add(this.factPath)

    this.render()
    // The dependency links are added by `render`
    for(const dependencyLink of this.querySelectorAll(`.${AuditedFact.dependencyClassName}`)) {
      dependencyLink.addEventListener('click', this.trackDependencyListener)
    }
  }

  disconnectedCallback() {
    this.removeButton.removeEventListener('click', this.deleteListener)
    document.removeEventListener(`fg-update`, this.renderListener)

    for(const dependencyLink of this.querySelectorAll(`.${AuditedFact.dependencyClassName}`)) {
      dependencyLink.removeEventListener('click', this.trackDependencyListener)
    }

    AuditedFact.selectedFacts.delete(this.factPath)
  }

  render() {
    const definition = factGraph.dictionary.getDefinition(this.factPath)
    const fact = factGraph.get(this.factPath)

    this.factPathElem.innerText = this.factPath
    this.factTypeElem.innerText = definition.typeNode
    this.factValueElem.innerText = `${fact.hasValue ? fact.get.toString() + ' ' : ''}${fact.complete ? `[Complete]` : `[Incomplete]`}`;

    const xmlDefinition = factDictionaryXml.querySelector(`Fact[path="${this.abstractPath}"]`)
    const dependencyNodes = Array.from(xmlDefinition.querySelectorAll('Dependency'))
    const stringDefinition = AuditedFact.xmlSerializer.serializeToString(xmlDefinition).replace(/</g, '&lt;').replace(/>/g, '&gt;')

    const renderedDefinition = document.createElement('div')
    renderedDefinition.setAttribute('slot', 'definition')
    renderedDefinition.innerHTML = dependencyNodes.reduce((result, dependencyNode) => {
      const rawPath = dependencyNode.getAttribute('path')

      // If the dependency path is relative, expand it to the current collection
      const dependencyPath = rawPath.replace('..', this.abstractPath.replace(/\*\/.*/, `*`))

      if(rawPath.includes('*')) {
        // We can't automatically follow non-relative, abstract paths
        return result;
      } else {
        return result.replace(
          `path="${rawPath}"`,
          `path="<a class="${AuditedFact.dependencyClassName}" href="#${dependencyPath}">${rawPath}</a>"`
        )
      }
    }, stringDefinition)
    this.append(renderedDefinition)
  }
}
customElements.define('audited-fact', AuditedFact)

