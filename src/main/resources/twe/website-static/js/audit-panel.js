const parser = new DOMParser()
const XML_SERIALIZER = new XMLSerializer()

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
  const factPath = makeCollectionIdPath(path, collectionId)

  const existingFact = auditedFactsList.querySelector(`audited-fact[path="${factPath}"]`)
  if (existingFact) {
    return existingFact.scrollIntoView()
  }
  console.debug(`Tracking ${factPath}`)

  const auditedFact = document.createElement('audited-fact')
  auditedFact.setAttribute('path', path)
  auditedFact.setAttribute('collectionId', collectionId)

  auditedFactsList.appendChild(auditedFact)
  auditedFact.scrollIntoView()
}

function loadPaths() {
  const paths = factGraph.paths().sort()
  const options = paths.map((path) => `<option path=${path}>${path}</option>`)
  pathOptions.innerHTML += options
}

class AuditedFact extends HTMLElement {
  static factLinkClass = 'audit-panel__fact__definition__dependency'

  constructor() {
    super()

    this.deleteListener = () => this.remove()
    this.renderListener = () => this.render()
    this.handleLinksListener = (e) => {
      if (e.target?.classList.contains(AuditedFact.factLinkClass)) {
        return this.trackDependencyListener(e)
      }
    }

    this.trackDependencyListener = (e) => {
      e.preventDefault()
      trackFact(e.target.href.replace(/.*#\//, '/'), this.getAttribute('collectionid'))
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
    this.addEventListener('click', this.handleLinksListener)
    document.addEventListener(`fg-update`, this.renderListener)

    this.render()
  }

  disconnectedCallback() {
    this.removeButton.removeEventListener('click', this.deleteListener)
    this.removeEventListener('click', this.handleLinksListener)
    document.removeEventListener(`fg-update`, this.renderListener)
  }

  render() {
    const definition = factGraph.dictionary.getDefinition(this.factPath)
    const fact = factGraph.get(this.factPath)

    // Fill out the data fields
    this.factPathElem.innerText = this.factPath
    this.factTypeElem.innerText = definition.typeNode
    const factValueString = fact.hasValue ? fact.get.toString() + ' ' : ''
    const factCompleteString = fact.complete ? `[Complete]` : `[Incomplete]`
    this.factValueElem.innerText = `${factValueString} ${factCompleteString}`;

    // Serialize and sanitizie the fact definition for inclusion as HTML
    // We do this because the definition will have live <a> links in it
    const xmlDefinition = factDictionaryXml.querySelector(`Fact[path="${this.abstractPath}"]`)
    const stringDefinition = XML_SERIALIZER.serializeToString(xmlDefinition)
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')

    // Enhance the definition by adding links to dependencies
    const dependencyNodes = Array.from(xmlDefinition.querySelectorAll('Dependency'))
    const fullDefinition = dependencyNodes.reduce((result, dependencyNode) => {
      const rawPath = dependencyNode.getAttribute('path')

      // For now, we can't resolve abstract collection paths ("/jobs/*/income")
      if (rawPath.includes('*')) {
        return result
      }
      // but we can resolve relative paths ("../income")
      const resolvedPath = rawPath.replace('..', this.abstractPath.replace(/\*\/.*/, `*`))

      const link = `<a class="${AuditedFact.factLinkClass}" href="#${resolvedPath}">${rawPath}</a>`
      return result.replace(`path="${rawPath}"`, `path="${link}"`)
    }, stringDefinition)

    const definitionElement = document.createElement('div')
    definitionElement.setAttribute('slot', 'definition')
    definitionElement.innerHTML = fullDefinition

    this.append(definitionElement)
  }
}
customElements.define('audited-fact', AuditedFact)

// Add links to all the <fg-show>s
const fgShows = document.querySelectorAll('fg-show')
for (const fgShow of fgShows) {
  const link = document.createElement('a')
  link.classList.add(AuditedFact.factLinkClass)
  link.href = `#${fgShow.path}`
  link.onclick = (e) => { e.preventDefault(); trackFact(fgShow.path) }

  fgShow.parentElement.replaceChild(link, fgShow)
  link.append(fgShow)
}
