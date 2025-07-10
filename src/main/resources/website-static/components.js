import * as fg from './factgraph-3.1.0.js'

const text = document.getElementById('fact-dictionary').textContent
const factDictionary = fg.FactDictionaryFactory.importFromXml(text)
const factGraph = fg.GraphFactory.apply(factDictionary)

class FgQuestion extends HTMLElement {
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

    factGraph.save()
    document.dispatchEvent(new CustomEvent('fg-update'))
  }
}

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

window.factGraph = factGraph
customElements.define('fg-question', FgQuestion)
customElements.define('fg-display', FgDisplay)
