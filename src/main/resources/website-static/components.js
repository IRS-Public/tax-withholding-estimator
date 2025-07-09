class FgQuestion extends HTMLElement {
    connectedCallback() {
        this.type = this.getAttribute('type')
        this.path = this.getAttribute('path')

        console.log(`Registering path ${this.path} of type ${this.path}`)
    }
}

customElements.define('fg-question', FgQuestion)
