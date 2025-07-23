import * as fg from './factgraph-3.1.0.js'


const text = document.getElementById('fact-dictionary').textContent
const factDictionary = fg.FactDictionaryFactory.importFromXml(text)
let factGraph = fg.GraphFactory.apply(factDictionary)
window.factGraph = factGraph

// Unhide main
document.querySelector('main').classList.remove('hidden')

/*
 * <fg-set> - An input that sets a fact
 */
class FgSet extends HTMLElement {
  connectedCallback() {
    this.condition = this.getAttribute('condition')
    this.inputType = this.getAttribute('inputtype')
    this.inputs = this.querySelectorAll('input, select')

    switch (this.inputType) {
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

    // Show/hide based on conditions
    if (this.condition) {
      const value = factGraph.get(this.condition)
      const meetsCondition = (value.complete && value.get) === true
      if (!meetsCondition) {
        this.classList.add('hidden')
      } else {
        this.classList.remove('hidden')
      }
    }

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
    console.debug(factGraph.toJson())
    document.dispatchEvent(new CustomEvent('fg-update'))
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

