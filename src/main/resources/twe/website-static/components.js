import * as fg from './factgraph-3.1.0.js'

// Small wrapper until I have time to burn down some of the FG's sillier interfaces
class FactGraph {
  constructor() {
    const text = document.getElementById('fact-dictionary').textContent
    this.factDictionary = fg.FactDictionaryFactory.importFromXml(text)
    this.graph = fg.GraphFactory.apply(this.factDictionary)
  }

  get(path) {
    return this.graph.get(path)
  }

  set(path, value) {
    this.graph.set(path, value)
    this.graph.save()
    document.dispatchEvent(new CustomEvent('fg-update'))
  }

  toJson() {
    return this.graph.toJson()
  }

  update() {
    document.dispatchEvent(new CustomEvent('fg-update'))
  }

  reset() {
    this.graph = fg.GraphFactory.apply(this.factDictionary)
    this.update()
  }
}

// Create a new fact graph and attach it to the window
// This lets you use it in the console
const factGraph = new FactGraph()
window.factGraph = factGraph

class FgSet extends HTMLElement {
  connectedCallback() {
    this.inputType = this.getAttribute('inputtype')
    this.path = this.getAttribute('path')
    this.addEventListener('change', this);
    console.log(`Registering path ${this.path} of inputType ${this.inputType}`)
  }

  handleEvent(_event) {
    this.updateFactGraph()
  }

  updateFactGraph() {
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

class FgDisplay extends HTMLElement {
  connectedCallback() {
    this.pre = document.createElement('pre')
    this.appendChild(this.pre)
    document.addEventListener('fg-update', () => this.update())
    this.update()
  }

  update() {
    const json = factGraph.toJson()
    const prettyJson = JSON.stringify(JSON.parse(json), null, 2)
    this.pre.innerText = prettyJson
  }
}
customElements.define('fg-display', FgDisplay)

class FgShow extends HTMLElement {
  connectedCallback() {
    this.path = this.getAttribute('path')
    document.addEventListener('fg-update', () => this.update())
    this.update()
  }

  update() {
    const value = factGraph.get(this.path)
    if (value.complete === false) {
      this.innerHTML = `<span class="incomplete">[Missing Information]</span>`
    } else {
      this.innerText = value.get?.toString()
    }

    // Show/hide based on conditions
    const nodesWithConditions = document.querySelectorAll('fg-set[condition]')
    for (const node of nodesWithConditions) {
      const conditionPath = node.getAttribute('condition')
      const value = factGraph.get(conditionPath)
      const meetsCondition = (value.complete && value.get) === true
      if (!meetsCondition) {
        node.classList.add('hidden')
      } else {
        node.classList.remove('hidden')
      }
    }
  }
}
customElements.define('fg-show', FgShow)

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

