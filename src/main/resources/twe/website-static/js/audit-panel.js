const parser = new DOMParser()
const XML_SERIALIZER = new XMLSerializer()

const res = await fetch('/app/tax-withholding-estimator/resources/fact-dictionary.xml')
const text = await res.text()
const factDictionaryXml = parser.parseFromString(text, 'application/xml')
const factSelect = document.querySelector('#fact-select')

window.enableAuditMode = enable
window.disableAuditMode = disable
window.trackSelectedFact = trackSelectedFact
window.pathSelectListener = (event) => {
  if (event.key === 'Enter') trackSelectedFact()
}

class FactLink extends HTMLElement {
  connectedCallback () {
    this.path = this.getAttribute('path')
    this.collectionId = this.getAttribute('collectionId')

    const link = document.createElement('a')
    link.href = `#${this.path}`
    while (this.firstChild) { link.appendChild(this.firstChild) } // Move all children to the link
    link.onclick = () => {
      trackFact(this.path, this.collectionId)
      return false
    }
    this.replaceChildren(link)
  }
}
customElements.define('fact-link', FactLink)

class AuditedFact extends HTMLElement {
  constructor () {
    super()

    this.deleteListener = () => this.remove()
    this.renderListener = () => this.render()

    const templateContent = document.querySelector('#audit-panel__fact').content.cloneNode(true)
    this.attachShadow({ mode: 'open' })
    this.shadowRoot.append(templateContent)

    this.factPathElem = this.shadowRoot.querySelector('.audit-panel__fact__path')
    this.factTypeElem = this.shadowRoot.querySelector('.audit-panel__fact__type')
    this.factValueElem = this.shadowRoot.querySelector('.audit-panel__fact__value')
    this.factDefinitionElem = this.shadowRoot.querySelector('.audit-panel__fact__definition')

    this.removeButton = this.shadowRoot.querySelector('.audit-panel__fact__remove')
  }

  connectedCallback () {
    this.abstractPath = this.getAttribute('path')
    this.collectionId = this.getAttribute('collectionid')
    this.factPath = makeCollectionIdPath(this.abstractPath, this.collectionId)

    this.removeButton.addEventListener('click', this.deleteListener)
    this.addEventListener('click', this.handleLinksListener)
    document.addEventListener('fg-update', this.renderListener)

    this.render()
  }

  disconnectedCallback () {
    this.removeButton.removeEventListener('click', this.deleteListener)
    this.removeEventListener('click', this.handleLinksListener)
    document.removeEventListener('fg-update', this.renderListener)
    factSelect.focus()
  }

  render () {
    const definition = window.factGraph.dictionary.getDefinition(this.factPath)
    const fact = window.factGraph.get(this.factPath)

    // Fill out the data fields
    this.factPathElem.innerText = this.factPath
    this.factTypeElem.innerText = definition.typeNode
    const factValueString = fact.hasValue ? fact.get.toString() + ' ' : ''
    const factCompleteString = fact.complete ? '[Complete]' : '[Incomplete]'
    this.factValueElem.innerText = `${factValueString} ${factCompleteString}`

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
      const abstractPath = rawPath.replace('..', this.abstractPath.replace(/\*\/.*/, '*'))
      const link = `<fact-link path="${abstractPath}" collectionId="${this.collectionId}">${rawPath}</fact-link>`
      return result.replace(`path="${rawPath}"`, `path="${link}"`)
    }, stringDefinition)

    const definitionElement = document.createElement('div')
    definitionElement.setAttribute('slot', 'definition')
    definitionElement.innerHTML = fullDefinition

    this.append(definitionElement)
  }
}
customElements.define('audited-fact', AuditedFact)

function trackSelectedFact () {
  const factPath = factSelect.value
  const collectionId = document.querySelector('#fact-collection-id').value
  if (factPath) {
    trackFact(factPath, collectionId)
    factSelect.value = ''
  }
}

function trackFact (path, collectionId) {
  const factPath = makeCollectionIdPath(path, collectionId)
  const auditedFactsList = document.querySelector('#audit-panel__fact-list')

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
  auditedFact.setAttribute('tabindex', '-1')
  auditedFact.focus()

  auditedFact.addEventListener('focusout', () => {
    auditedFact.removeAttribute('tabindex')
  }, { once: true })
}

function setFactOptions () {
  const paths = window.factGraph.paths().sort()
  const options = paths.map((path) => `<option path=${path}>${path}</option>`)
  document.querySelector('#fact-options').innerHTML = options
}

function makeCollectionIdPath (abstractPath, id) {
  return abstractPath.replace('*', `#${id}`)
}

async function copyFactGraphToClipboard () {
  const fg = window.factGraph.toJson()
  const status = document.getElementById('copy-fg-status')
  try {
    await navigator.clipboard.writeText(fg)
    status.classList.add('animate-success')
    status.onanimationend = () => {
      status.classList.remove('animate-success')
    }
  } catch (err) {
    console.error(`Failed to copy: ${err}`)
  }
}
window.copyFactGraphToClipboard = copyFactGraphToClipboard

export function enable () {
  document.querySelector('#audit-panel-styles').disabled = false
  document.querySelector('#audit-panel').classList.remove('hidden')

  // Add links to all the <fg-show>s
  const fgShows = document.querySelectorAll('fg-show')
  for (const fgShow of fgShows) {
    const factLink = document.createElement('fact-link')
    factLink.setAttribute('path', fgShow.path)
    factLink.append(fgShow.cloneNode())
    fgShow.parentElement.replaceChild(factLink, fgShow)
  }

  // Load fact paths once the fact graph is available (if it isn't already)
  if (!window.factGraph) {
    document.addEventListener('fg-load', setFactOptions)
  } else {
    setFactOptions()
  }
}

export function disable () {
  document.querySelector('#audit-panel-styles').disabled = true
  document.querySelector('#audit-panel').classList.add('hidden')

  // Remove links from all the <fg-show>s
  const fgShows = document.querySelectorAll('fg-show')
  for (const fgShow of fgShows) {
    const link = fgShow.parentElement
    link.parentElement.replaceChild(fgShow, link)
  }
}

/* Attempt to load the Fact Graph and set a validation error if it fails
 *
 * It's important that this function either succeeds and triggers a new page load, or fails and sets
 * a validation message. Otherwise the form will attempt to "submit," accomplishing nothing. It
 * works this way because the custom validation message has to be set before the 'submit' event
 * fires (as far as I can tell).
 */
function loadFactGraphFromAuditPanel () {
  const textarea = document.querySelector('#load-fact-graph')
  const formGroup = textarea.closest('.usa-form-group')
  const label = formGroup.querySelector('.usa-label')
  let errorMessage = formGroup.querySelector('#load-fact-graph-error')

  try {
    window.loadFactGraph(textarea.value)
    if (errorMessage) {
      errorMessage.remove()
      textarea.classList.remove('usa-input--error')
      textarea.removeAttribute('aria-describedby')
      label.classList.remove('usa-label--error')
      formGroup.classList.remove('usa-form-group--error')
    }
  } catch (error) {
    const errorMessageId = 'load-fact-graph-error'
    if (!errorMessage) {
      errorMessage = document.createElement('span')
      errorMessage.id = errorMessageId
      errorMessage.className = 'usa-error-message'
      label.after(errorMessage)
      errorMessage.innerText = 'Enter a valid JSON'
      label.classList.add('usa-label--error')
      textarea.classList.add('usa-input--error')
      textarea.setAttribute('aria-describedby', errorMessageId)
      formGroup.classList.add('usa-form-group--error')
      textarea.focus()
    }
  }
}
window.loadFactGraphFromAuditPanel = loadFactGraphFromAuditPanel
