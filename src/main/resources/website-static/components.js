import * as fg from './factgraph-3.1.0.js'

const factGraph = fg.GraphFactory.apply()

class FgQuestion extends HTMLElement {
    connectedCallback() {
        this.inputType = this.getAttribute('inputType')
        this.path = this.getAttribute('path')

        console.log(`Registering path ${this.path} of inputType ${this.inputType}`)
    }
}

customElements.define('fg-question', FgQuestion)
