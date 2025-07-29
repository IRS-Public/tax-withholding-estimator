import * as fg from './factgraph-3.1.0.js'

const INFINITE_LOOP_THRESHOLD = 100

const text = document.getElementById('fact-dictionary').textContent
const factDictionary = fg.FactDictionaryFactory.importFromXml(text)
let factGraph = fg.GraphFactory.apply(factDictionary)
window.factGraph = factGraph

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
      case 'day':
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

    document.addEventListener('fg-update', () => this.render());
    this.render()
  }

  render() {
    this.querySelector('div.warning')?.remove()

    // Show error if there is one
    if (this.error) {
      const warnDiv = document.createElement('div')
      warnDiv.classList.add('warning')
      warnDiv.innerText = this.error
      this.insertAdjacentElement('afterbegin', warnDiv)
    }
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

  setFact() {
    console.debug(`Setting fact ${this.path}`)
    switch (this.inputType) {
      case 'boolean': {
        const input = this.querySelector('input:checked')
        factGraph.set(this.path, input.value)
        break
      }
      case 'day':
      case 'dollar': {
        const input = this.querySelector('input')
        factGraph.set(this.path, input.value)
        break
      }
      case 'select': {
        const input = this.querySelector('select')
        factGraph.set(this.path, input.value)
        break
      }
      default: {
        console.warn(`Unknown input type "${this.inputType}" for input with path "${this.path}"`)
      }
    }

    factGraph.save()
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
      case 'day':
      case 'select':
      case 'dollar': {
        const input = this.querySelector('input')
        input.value = ''
        break
      }
      default: {
        console.warn(`Unknown input type "${this.inputType}" for input with path "${this.path}"`)
      }
    }
    factGraph.delete(this.path)
    factGraph.save()
  }

}
customElements.define('fg-set', FgSet)

/*
 * <fg-collection> - Expandable collection list
 */
class FgCollection extends HTMLElement {
  connectedCallback() {
    this.childSetters = this.innerHTML
    this.innerHTML = ""

    this.path = this.getAttribute('path')

    this.addItemButton = document.createElement('button')
    this.addItemButton.innerText = 'Add Item'
    this.addItemButton.addEventListener(('click'), () => this.addItem())
    this.appendChild(this.addItemButton)
  }

  addItem() {
      const fieldset = document.createElement('fieldset')
      const collectionId = crypto.randomUUID()

      factGraph.addToCollection(this.path, collectionId)
      factGraph.save()

      const collectionItem = document.createElement('fg-collection-item')
      collectionItem.innerHTML = this.childSetters
      collectionItem.collectionId = collectionId

      fieldset.appendChild(collectionItem)
      this.appendChild(fieldset)
  }

}
customElements.define('fg-collection', FgCollection)

class FgCollectionItem extends HTMLElement {
  connectedCallback() {
    this.collectionId = this.collectionId

    const setters = this.querySelectorAll('fg-set')
    for (const setter of setters) {
      const pathWithWildcard = setter.getAttribute('path')
      const fullPath = pathWithWildcard.replace('*', '#' + this.collectionId)
      setter.setAttribute('path', fullPath)
    }
  }
}
customElements.define('fg-collection-item', FgCollectionItem)

/*
 * <fg-show> - Display the current value of a fact.
 */
class FgShow extends HTMLElement {
  connectedCallback() {
    this.path = this.getAttribute('path')
    document.addEventListener('fg-update', () => this.render())
    this.render()
  }

  render() {
    const value = factGraph.get(this.path)
    if (value.complete === false) {
      this.innerHTML = `<span class="incomplete">[Missing Information]</span>`
    } else {
      this.innerText = value.get?.toString()
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
    const inputs = document.querySelectorAll('fg-set input')
    for (const input of inputs) {
      input.value = ""
    }
    factGraph = fg.GraphFactory.apply(factDictionary)
    window.factGraph = factGraph
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
  // At present, this navie implementation relies on <fg-set>s not having conditions on facts that
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

// Add show/hide functionality to all elements
document.addEventListener('fg-update', showOrHideAllElements)
showOrHideAllElements()

// Unhide main
document.querySelector('main').classList.remove('hidden')

