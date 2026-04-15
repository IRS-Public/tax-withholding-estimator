import { displayConditions, hideConditions } from './fg-components.js'

const parser = new DOMParser()
const XML_SERIALIZER = new XMLSerializer()

const res = await fetch('/app/tax-withholding-estimator/resources/fact-dictionary.xml')
const text = await res.text()
const factDictionaryXml = parser.parseFromString(text, 'application/xml')
const factSelect = document.querySelector('#fact-select')
const openAuditPanelButton = document.querySelector('#show-audit-panel')
const closeAuditPanelButton = document.querySelector('#close-audit-panel')
const auditPanel = document.querySelector('#audit-panel')
const auditPanelResizer = document.querySelector('#audit-panel-resizer')
const AUDIT_PANEL_STORAGE_KEY = 'auditPanel'
const AUDIT_PANEL_STORAGE_FIELDS = new Set(['isOpen', 'trackedFacts', 'showConditions', 'width'])
const AUDIT_PANEL_DEFAULT_WIDTH = 38
const AUDIT_PANEL_MIN_WIDTH = 320
const AUDIT_PANEL_MAX_WIDTH_RATIO = 0.7
const AUDIT_PANEL_KEYBOARD_STEP = 24

// Save the open/closed state of the audit panel in session storage so it persists across page reloads and forward navigation.
function getAuditPanelStorage () {
  const storage = sessionStorage.getItem(AUDIT_PANEL_STORAGE_KEY)
  if (storage) {
    return JSON.parse(storage)
  } else {
    return {}
  }
}

// Set a key/value pair in session storage for the audit panel, with special handling to ensure tracked facts are unique by path and collectionId
function setAuditPanelStorage (key, value) {
  if (!AUDIT_PANEL_STORAGE_FIELDS.has(key)) {
    throw new Error(`Unsupported audit panel storage key: ${key}`)
  }

  const storage = getAuditPanelStorage()
  if (key === 'trackedFacts') {
    const uniqueFacts = []
    const seen = new Set()
    for (const fact of value) {
      const factId = `${fact.path}#${fact.collectionId}`
      if (!seen.has(factId)) {
        uniqueFacts.push(fact)
        seen.add(factId)
      }
    }
    storage.trackedFacts = uniqueFacts
  } else if (key === 'isOpen') {
    storage.isOpen = value
  } else if (key === 'showConditions') {
    storage.showConditions = value
  } else if (key === 'width') {
    storage.width = value
  }
  sessionStorage.setItem(AUDIT_PANEL_STORAGE_KEY, JSON.stringify(storage))
}

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
      document.body.classList.add('audit-panel-open')
      setAuditPanelStorage('isOpen', true)
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

    this.deleteListener = () => {
      const storage = getAuditPanelStorage()
      const trackedFacts = storage.trackedFacts || []
      const newTrackedFacts = trackedFacts.filter((fact) => fact.path !== this.abstractPath && fact.collectionId !== this.collectionId)
      setAuditPanelStorage('trackedFacts', newTrackedFacts)
      this.remove()
    }
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

    // Serialize and sanitize the fact definition for inclusion as HTML
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

    this.querySelector('[slot="definition"]')?.remove()
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

// Add a fact to the audit panel to track
function trackFact (path, collectionId, setFocus = true) {
  const factPath = makeCollectionIdPath(path, collectionId)
  const auditedFactsList = document.querySelector('#audit-panel__fact-list')

  const existingFact = auditedFactsList.querySelector(`audited-fact[path="${factPath}"]`)
  if (existingFact) {
    return existingFact.scrollIntoView()
  }
  console.debug(`Tracking ${factPath}`)

  // Store the tracked fact in session storage so it persists across page reloads with forward/back navigation
  const storage = getAuditPanelStorage()
  const trackedFacts = storage.trackedFacts || []
  trackedFacts.push({ path, collectionId })
  setAuditPanelStorage('trackedFacts', trackedFacts)

  const auditedFact = document.createElement('audited-fact')
  auditedFact.setAttribute('path', path)
  auditedFact.setAttribute('collectionId', collectionId)

  auditedFactsList.appendChild(auditedFact)
  auditedFact.scrollIntoView()

  // Set focus to the newly added fact for accessibility, and remove the tabindex after focus is lost so the fact doesn't remain in the tab order unnecessarily
  if (setFocus) {
    auditedFact.setAttribute('tabindex', '-1')
    auditedFact.focus()

    auditedFact.addEventListener('focusout', () => {
      auditedFact.removeAttribute('tabindex')
    }, { once: true })
  }
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

// Enable audit mode
export function enable () {
  // This focus handling is a bit of a hack, but it ensures that the track facts in the audit panel are not stealing focus when navigating with the keyboard.
  document.documentElement.tabIndex = -1
  document.documentElement.focus()
  document.documentElement.addEventListener('focusout', () => {
    document.documentElement.removeAttribute('tabindex')
  }, { once: true })

  // Set up the audit to display on the page and display the open button
  document.querySelector('#audit-panel-styles').disabled = false
  document.querySelector('#audit-panel').classList.remove('hidden')
  openAuditPanelButton.classList.remove('hidden')

  // Set up adjustable width controls for the audit panel
  function initializeAdjustableWidth () {
    if (!auditPanel) {
      return () => {}
    }

    if (auditPanel.dataset.widthControlsInitialized === 'true' && typeof auditPanel.syncAuditPanelWidth === 'function') {
      return auditPanel.syncAuditPanelWidth
    }

    function getAuditPanelMaxWidth () {
      return Math.max(AUDIT_PANEL_MIN_WIDTH, Math.floor(window.innerWidth * AUDIT_PANEL_MAX_WIDTH_RATIO))
    }

    function clampAuditPanelWidth (width) {
      return Math.min(Math.max(width, AUDIT_PANEL_MIN_WIDTH), getAuditPanelMaxWidth())
    }

    function updateAuditPanelResizerAccessibility (width) {
      if (!auditPanelResizer) {
        return
      }

      const maxWidth = getAuditPanelMaxWidth()
      auditPanelResizer.setAttribute('aria-valuemin', String(AUDIT_PANEL_MIN_WIDTH))
      auditPanelResizer.setAttribute('aria-valuemax', String(maxWidth))
      auditPanelResizer.setAttribute('aria-valuenow', String(width))
      auditPanelResizer.setAttribute('aria-valuetext', `${width}px wide`)
    }

    function applyAuditPanelWidth (width, persist = true) {
      const nextWidth = clampAuditPanelWidth(width)
      document.documentElement.style.setProperty('--audit-panel-width', `${nextWidth}px`)
      updateAuditPanelResizerAccessibility(nextWidth)

      if (persist) {
        setAuditPanelStorage('width', nextWidth)
      }

      return nextWidth
    }

    function applyDefaultAuditPanelWidth () {
      document.documentElement.style.setProperty('--audit-panel-width', `${AUDIT_PANEL_DEFAULT_WIDTH}vw`)
      const fallbackWidth = Math.round(window.innerWidth * AUDIT_PANEL_DEFAULT_WIDTH / 100)
      const renderedWidth = Math.round(auditPanel.getBoundingClientRect().width) || fallbackWidth
      updateAuditPanelResizerAccessibility(clampAuditPanelWidth(renderedWidth))
    }

    function resizeAuditPanelBy (delta) {
      const currentWidth = Math.round(auditPanel.getBoundingClientRect().width)
      return applyAuditPanelWidth(currentWidth + delta)
    }

    function handleAuditPanelResizeKeydown (event) {
      if (event.key === 'ArrowRight') {
        event.preventDefault()
        resizeAuditPanelBy(-AUDIT_PANEL_KEYBOARD_STEP)
      } else if (event.key === 'ArrowLeft') {
        event.preventDefault()
        resizeAuditPanelBy(AUDIT_PANEL_KEYBOARD_STEP)
      }
    }

    function handleAuditPanelResizerPointerDown (event) {
      if (event.button !== 0 || !auditPanelResizer) {
        return
      }

      event.preventDefault()
      auditPanelResizer.setPointerCapture(event.pointerId)
      document.body.classList.add('audit-panel-resizing')

      const handlePointerMove = (moveEvent) => {
        applyAuditPanelWidth(window.innerWidth - moveEvent.clientX)
      }

      const handlePointerUp = () => {
        auditPanelResizer.releasePointerCapture(event.pointerId)
        document.body.classList.remove('audit-panel-resizing')
        window.removeEventListener('pointermove', handlePointerMove)
        window.removeEventListener('pointerup', handlePointerUp)
      }

      window.addEventListener('pointermove', handlePointerMove)
      window.addEventListener('pointerup', handlePointerUp)
    }

    function syncAuditPanelWidth () {
      const storage = getAuditPanelStorage()
      if (typeof storage.width === 'number') {
        applyAuditPanelWidth(storage.width)
      } else {
        applyDefaultAuditPanelWidth()
      }
    }

    auditPanelResizer?.addEventListener('pointerdown', handleAuditPanelResizerPointerDown)
    auditPanelResizer?.addEventListener('keydown', handleAuditPanelResizeKeydown)
    window.addEventListener('resize', syncAuditPanelWidth)

    auditPanel.dataset.widthControlsInitialized = 'true'
    auditPanel.syncAuditPanelWidth = syncAuditPanelWidth

    return syncAuditPanelWidth
  }

  // Initialize the adjustable width controls and sync the width to storage or the default value
  const syncAuditPanelWidth = initializeAdjustableWidth()
  syncAuditPanelWidth()

  // Set up function to open the audit panel
  function openAuditPanel () {
    document.body.classList.add('audit-panel-open')
    setAuditPanelStorage('isOpen', true)
    closeAuditPanelButton.focus()
  }

  // Set up function to close the audit panel
  function closeAuditPanel () {
    document.body.classList.remove('audit-panel-open')
    setAuditPanelStorage('isOpen', false)
    openAuditPanelButton.focus()
  }

  // Set up function to close the audit panel with the escape key
  function handleAuditPanelKeydown (event) {
    if (event.key === 'Escape' && document.body.classList.contains('audit-panel-open')) {
      event.preventDefault()
      closeAuditPanel()
    }
  }

  // Add event listeners for opening and closing the audit panel, and for handling escape key presses to close the panel
  if (auditPanel?.dataset.visibilityControlsInitialized !== 'true') {
    openAuditPanelButton.addEventListener('click', openAuditPanel)
    closeAuditPanelButton.addEventListener('click', closeAuditPanel)
    document.addEventListener('keydown', handleAuditPanelKeydown)
    auditPanel.dataset.visibilityControlsInitialized = 'true'
  }

  // If the audit panel was previously open, make sure to open it again when navigating forward or backwards
  if (getAuditPanelStorage().isOpen) {
    document.body.classList.add('audit-panel-open')
  }

  // If there are any facts stored in session storage, make sure to add them back
  const storage = getAuditPanelStorage()
  if (storage.trackedFacts) {
    for (const fact of storage.trackedFacts) {
      trackFact(fact.path, fact.collectionId, false)
    }
  }

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

  // Set up the show conditions toggle
  const conditionsCheckbox = document.querySelector('#show-conditions')
  conditionsCheckbox.addEventListener('change', () => {
    setAuditPanelStorage('showConditions', conditionsCheckbox.checked)
    if (conditionsCheckbox.checked) {
      displayConditions()
    } else {
      hideConditions()
    }
  })

  // If the user had show conditions toggled on, make sure to show them and set up the listener for new collections added after page load
  if (getAuditPanelStorage().showConditions) {
    conditionsCheckbox.checked = true
    displayConditions()
    document.querySelectorAll('.fg-collection__add-item').forEach((element) => {
      element.addEventListener('click', () => {
        hideConditions()
        displayConditions()
      })
    })
  }
}

// Disable audit mode and clear all tracked facts and stored state
export function disable () {
  document.querySelector('#audit-panel-styles').disabled = true
  document.querySelector('#audit-panel').classList.add('hidden')
  openAuditPanelButton.classList.add('hidden')
  document.body.classList.remove('audit-panel-open')
  document.body.removeAttribute('style')
  sessionStorage.removeItem(AUDIT_PANEL_STORAGE_KEY)
  hideConditions()

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
