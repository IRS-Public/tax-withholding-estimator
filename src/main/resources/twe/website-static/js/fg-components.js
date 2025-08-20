import * as fg from './factgraph-3.1.0.js'

const text = document.getElementById('fact-dictionary').textContent
const factDictionary = fg.FactDictionaryFactory.importFromXml(text)

let factGraph
const serializedGraphJSON = sessionStorage.getItem('factGraph')
if (serializedGraphJSON) {
  factGraph = fg.GraphFactory.fromJSON(factDictionary, serializedGraphJSON)
} else {
  factGraph = fg.GraphFactory.apply(factDictionary)
}
window.factGraph = factGraph

function saveFactGraph() {
  sessionStorage.setItem('factGraph', factGraph.toJSON())
}

/**
 * @param {string} factGraphAsString - stringified version of a JSON object
 */
function loadFactGraph(factGraphAsString) {
  factGraph = fg.GraphFactory.fromJSON(factDictionary, factGraphAsString)
  saveFactGraph()
  window.location.reload()
}
window.loadFactGraph = loadFactGraph

function makeCollectionIdPath(abstractPath, id) {
  return abstractPath.replace('*', `#${id}`);
}

/*
 * <fg-set> - An input that sets a fact
 */
class FgSet extends HTMLElement {
  connectedCallback() {
    this.condition = this.getAttribute('condition')
    this.operator = this.getAttribute('operator')
    this.inputType = this.getAttribute('inputtype')
    this.inputs = this.querySelectorAll('input, select')

    switch (this.inputType) {
      // This switch statement is intentionally not exhaustive
      case 'date':
      case 'select':
      case 'boolean':
        for (const input of this.inputs) {
          input.addEventListener('change', () => this.onChange());
        }
        break;
      default:
        for (const input of this.inputs) {
          input.addEventListener('blur', () => this.onChange());
        }
    }

    this.path = this.getAttribute('path')
    this.error = null

    console.debug(`Adding fg-set with path ${this.path} of inputType ${this.inputType}`)

    // This is done with bind, rather than an arrow function, so that it can be removed later
    this.render = this.render.bind(this)
    this.clear = this.clear.bind(this)

    document.addEventListener('fg-update', this.render);
    document.addEventListener('fg-clear', this.clear);
    this.render()
  }

  disconnectedCallback() {
    console.debug(`Removing fg-set with path ${this.path}`)
    document.removeEventListener('fg-update', this.render)
    document.removeEventListener('fg-clear', this.clear);
  }

  render() {
    this.querySelector('div.alert--warning')?.remove()

    // Show error if there is one
    if (this.error) {
      const warnDiv = document.createElement('div')
      warnDiv.classList.add('alert--warning')
      warnDiv.innerText = this.error
      this.insertAdjacentElement('afterbegin', warnDiv)
    }

    this.setInputValueFromFactValue()
  }

  onChange() {
    try {
      this.setFact()
      this.error = null
    } catch (error) {
      console.error(error)
      this.error = error.message
      return
    } finally {
      this.render()
    }
  }

  isComplete() {
    return factGraph.get(this.path).complete
  }

  clear() {
    switch (this.inputType) {
      case 'boolean': {
        const checkedRadio = this.querySelector(`input:checked`)
        if (checkedRadio) checkedRadio.checked = false
        break
      }
      case 'select': {
        this.querySelector('select').value = ''
        break
      }
      case 'text':
      case 'date':
      case 'int':
      case 'dollar': {
        this.querySelector('input').value = ''
        break
      }
      default: {
        console.warn(`Unknown input type "${this.inputType}" for input with path "${this.path}"`)
      }
    }

    // Clear all errors and re-render
    this.error = null;
    this.render();
  }

  setInputValueFromFactValue() {
    // Don't attempt to set non-normalized wildcard facts
    if (this.path.includes('*')) return

    let fact = factGraph.get(this.path)

    let value
    if (fact.hasValue === false) {
      value = ""
    } else {
      value = fact.get?.toString()
    }

    switch (this.inputType) {
      case 'boolean': {
        if (value !== "") {
          this.querySelector(`input[value=${value}]`).checked = true
        }
        break
      }
      case 'select': {
        this.querySelector('select').value = value
        break
      }
      case 'text':
      case 'int':
      case 'date':
      case 'dollar': {
        this.querySelector('input').value = value
        break
      }
      default: {
        console.warn(`Unknown input type "${this.inputType}" for input with path "${this.path}"`)
      }
    }
  }

  getFactValueFromInputValue() {
    switch (this.inputType) {
      case 'boolean': {
        return this.querySelector('input:checked')?.value
      }
      case 'select': {
        return this.querySelector('select')?.value
      }
      case 'text':
      case 'date':
      case 'int':
      case 'dollar': {
        return this.querySelector('input')?.value
      }
      default: {
        console.warn(`Unknown input type "${this.inputType}" for input with path "${this.path}"`)
        return undefined
      }
    }
  }

  setFact() {
    console.debug(`Setting fact ${this.path}`)
    const value = this.getFactValueFromInputValue()

    if (value === "") {
      console.debug(`Value was blank, deleting fact`);
      factGraph.delete(this.path);
    } else {
      factGraph.set(this.path, value);
    }

    saveFactGraph()
    document.dispatchEvent(new CustomEvent('fg-update'))
  }

  /**
   * Deletes the current fact without sending fg-update.
   *
   * This method is used when processing the fg-update event, to delete facts that are no longer
   * visible. It will get called multiple times per fg-update, because deleting some facts may
   * trigger other facts to get deleted. It does not itself dispatch fg-update, because that would
   * throw off a lot of unnecessary events.
   *
   * At present, it is impossible for users to delete facts, so deleting a fact should never trigger
   * a new UI update.
   */
  deleteFactNoUpdate() {
    console.debug(`Deleting fact ${this.path}`)

    switch (this.inputType) {
      case 'boolean': {
        const input = this.querySelector('input:checked')
        if (input) input.checked = false
        break
      }
      case 'select': {
        this.querySelector('select').value = ''
      }
      case 'text':
      case 'date':
      case 'dollar': {
        this.querySelector('input').value = ''
        break
      }
      default: {
        console.warn(`Unknown input type "${this.inputType}" for input with path "${this.path}"`)
      }
    }
    factGraph.delete(this.path)
    saveFactGraph()
  }

}
customElements.define('fg-set', FgSet)

/*
 * <fg-collection> - Expandable collection list
 */
class FgCollection extends HTMLElement {
  connectedCallback() {
    this.itemChildren = this.innerHTML
    this.innerHTML = ""

    this.path = this.getAttribute('path')

    this.addItemButton = document.createElement('button')
    this.addItemButton.innerText = 'Add Item'
    this.addItemButton.addEventListener(('click'), () => this.addItem())
    this.appendChild(this.addItemButton)

    // Add any items that currently exist in this collection
    const ids = factGraph.getCollectionIds(this.path)
    ids.map(id => this.addItem(id))
  }

  addItem(id) {
    const fieldset = document.createElement('fieldset')

    let collectionId = id
    if (!collectionId) {
      collectionId = crypto.randomUUID()
      factGraph.addToCollection(this.path, collectionId)
      saveFactGraph()
    }

    const collectionItem = document.createElement('fg-collection-item')
    collectionItem.itemChildren = this.itemChildren
    collectionItem.path = makeCollectionIdPath(`${this.path}/*`, collectionId);
    collectionItem.collectionId = collectionId

    fieldset.appendChild(collectionItem)
    this.appendChild(fieldset)
    document.dispatchEvent(new CustomEvent('fg-update'))
  }

}
customElements.define('fg-collection', FgCollection)

class FgCollectionItem extends HTMLElement {
  connectedCallback() {
    this.collectionId = this.collectionId

    // It's important to the lifecycle of these elements that we create them in a template:
    // template.insertAdjacentHTML parses and creates the children, but they're not in the DOM yet.
    // This gives FgCollectionItem a chance to update the paths of all <fg-set> children before the
    // <fg-set> elements run their connectedCallback, which needs the full path, with an ID
    const template = document.createElement('template')
    template.insertAdjacentHTML('afterbegin', this.itemChildren)

    this.clearListener = () => this.clear();
    document.addEventListener(`fg-clear`, this.clearListener);

    const setters = template.querySelectorAll('fg-set')
    // These are all the attributes that we need to update from collection/*/fact to collection/#id/fact
    const attributes = ['path', 'condition']
    for (const setter of setters) {
      for (const attribute of attributes) {
        const attributeWithWildcard = setter.getAttribute(attribute)
        if (attributeWithWildcard) {
          const attributeWithId = attributeWithWildcard.replace('*', '#' + this.collectionId)
          setter.setAttribute(attribute, attributeWithId)
        }
      }
    }

    for (const child of template.children) {
      this.appendChild(child.cloneNode(true))
    }

    this.removeItemListener = () => {
      this.clear();
      this.dispatchEvent(new CustomEvent(`fg-update`));
    }

    const removeButton = document.createElement(`button`);
    removeButton.innerText = `Remove item`;
    removeButton.addEventListener(`click`, this.removeItemListener);

    this.prepend(removeButton)
  }

  clear() {
    for(const fgSet of this.querySelectorAll(customElements.getName(FgSet))) {
      fgSet.remove();
    }

    factGraph.delete(this.path);
    saveFactGraph()

    // Remove this element and its parent fieldset from the DOM after removing the item from the fact graph
    this.parentElement.remove();
  }
}
customElements.define('fg-collection-item', FgCollectionItem)

/*
 * <fg-show> - Display the current value and/or status of a fact.
 */
class FgShow extends HTMLElement {
  connectedCallback() {
    this.path = this.getAttribute('path')
    document.addEventListener('fg-update', () => this.render())
    this.render()
  }

  render() {
    const result = factGraph.get(this.path)

    if (result.hasValue) {
      const value = result.get.toString()
      if (result.complete === false) {
        this.innerHTML = `${value}&nbsp;<span class="text-base-light">[Placeholder value]</span>`
      } else {
        this.innerText = value
      }
    } else {
      this.innerHTML = `<span class="text-base-light">[Fact has no value]</span>`
    }
  }
}
customElements.define('fg-show', FgShow)

/*
 * <fg-reset> - button to reset the Fact Graph.
 */
class FgReset extends HTMLElement {
  connectedCallback() {
    this.addEventListener('click', this)
  }

  handleEvent() {
    factGraph = fg.GraphFactory.apply(factDictionary)
    window.factGraph = factGraph
    saveFactGraph()

    showOrHideAllElements()
    document.dispatchEvent(new CustomEvent('fg-clear'))
  }
}
customElements.define('fg-reset', FgReset)

function checkCondition(condition, operator) {
  const value = factGraph.get(condition)

  switch (operator) {
    // We need to expliticly check for true/false to account for incompletes
    case 'isTrue': {
      return value.hasValue && (value.get === true)
    } case 'isFalse': {
      return value.hasValue && (value.get === false)
    } default: {
      console.error(`Unknown condition operator ${operator}`)
      return false
    }
  }
}

/**
 * Show or hide the elements in the document based on the Fact Graph config.
 *
 * This method will delete facts that are hidden, making them incomplete.
 */
function showOrHideAllElements() {
  // At present, this naive implementation relies on <fg-set>s not having conditions on facts that
  // are set after them in the DOM order. This is a deliberate choice to limit complexity at this
  // stage, but it is not set in stone. If you see bugs related to showing/hiding, this is the place
  // to start looking.
  const hideableElements = document.querySelectorAll('[condition][operator]')
  for (const element of hideableElements) {
    const condition = element.getAttribute('condition')
    const operator = element.getAttribute('operator')
    const meetsCondition = checkCondition(condition, operator)

    // Show/hide based on conditions
    if (!meetsCondition && !element.classList.contains('hidden')) {
      element.classList.add('hidden')
      element?.deleteFactNoUpdate()
    } else if (meetsCondition && element.classList.contains('hidden')) {
      element.classList.remove('hidden')
    }
  }
}

function handleSectionContinue(event) {
  if (!validateSectionForNavigation()) {
    event.preventDefault();
    return false;
  }
  return true;
}

function handleSectionComplete(event) {
  event.preventDefault();
  if (validateSectionForNavigation()) {
    alert("You've completed filling out the Tax Withholding Estimator");
  }
  return false;
}

function validateSectionForNavigation() {
  const fgSets = document.querySelectorAll('fg-set:not(.hidden)');
  const missingFields = [];

  // Loop through fields and mark incomplete if empty
  for (const fgSet of fgSets) {
    if (!fgSet.isComplete()) {
      const fieldName = fgSet.path;
      missingFields.push(fieldName);
    }
  }
  // Display validation error if there are missing fields/incomplete
  if (missingFields.length > 0) {
    showValidationError();
    return false;
  }

  return true;
}

function showValidationError() {
  // Target custom class validate-alert
  const existingAlert = document.querySelector('.validate-alert');
  if (existingAlert) {
    existingAlert.remove();
  }
  // Clone the alert
  const template = document.getElementById('validate-alert-template');
  const alertElement = template.content.cloneNode(true);
  const mainContent = document.getElementById('main-content');
  // Place the alert at the top of the main content
  mainContent.insertBefore(alertElement, mainContent.firstChild);

}

window.handleSectionContinue = handleSectionContinue;
window.handleSectionComplete = handleSectionComplete;

// Add show/hide functionality to all elements
document.addEventListener('fg-update', showOrHideAllElements)
showOrHideAllElements()

// Unhide main
document.querySelector('main').classList.remove('hidden')
