import * as fg from './factgraph-3.1.0.js'

// Small wrapper until I have time to burn down some of the FG's sillier interfaces
class FactGraph {
  constructor() {
    const text = document.getElementById('fact-dictionary').textContent
    this.factDictionary = fg.FactDictionaryFactory.importFromXml(text)
    this.graph = fg.GraphFactory.apply(this.factDictionary)
    this.#update()
  }

  get(path) {
    return this.graph.get(path)
  }

  set(path, value) {
    try {
      this.graph.set(path, value)
      this.graph.save()
    } finally {
      this.#update()
    }
  }

  toJson() {
    return this.graph.toJson()
  }

  #update() {
    // Show/hide based on conditions
    const nodesWithConditions = document.querySelectorAll('fg-set[condition]')
    for (const node of nodesWithConditions) {
      const conditionPath = node.getAttribute('condition')
      const value = this.get(conditionPath)
      const meetsCondition = (value.complete && value.get) === true
      if (!meetsCondition) {
        node.classList.add('hidden')
      } else {
        node.classList.remove('hidden')
      }
    }

    document.dispatchEvent(new CustomEvent('fg-update'))
  }

  reset() {
    this.graph = fg.GraphFactory.apply(this.factDictionary)
    this.#update()
  }
}

// Create a new fact graph and attach it to the window
// This lets you use it in the console
const factGraph = new FactGraph()
window.factGraph = factGraph

// Unhide main
document.querySelector('main').classList.remove('hidden')

/*
 * <fg-set> - An input that sets a fact
 */
class FgSet extends HTMLElement {
  connectedCallback() {
    this.inputType = this.getAttribute('inputtype')
    this.input = this.querySelector('input, select')
    this.path = this.getAttribute('path')
    this.error = null

    this.input.addEventListener('blur', () => this.onChange());
    this.addEventListener('fg-update', () => this.render());
    this.render()

    console.log(`Registering fact ${this.path} of inputType ${this.inputType}`)
  }

  render() {
    if (this.error) {
      const warnDiv = document.createElement('div')
      warnDiv.classList.add('warning')
      warnDiv.innerText = this.error
      this.insertAdjacentElement('afterbegin', warnDiv)
    } else {
      this.querySelector('div.warning')?.remove()
    }
  }

  onChange() {
    try {
      this.setFact()
      this.error = null
    } catch (error) {
      this.error = error.message
      return
    } finally {
      this.render()
    }
  }

  setFact() {
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
  }
}
customElements.define('fg-set', FgSet)

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
    factGraph.reset()
  }
}
customElements.define('fg-reset', FgReset)

