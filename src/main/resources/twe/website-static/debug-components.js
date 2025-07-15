/*
 * <fg-json> - Display the entire Fact Graph as a JSON string.
 */
class FgJson extends HTMLElement {
  connectedCallback() {
    this.pre = document.createElement('pre')
    this.appendChild(this.pre)
    document.addEventListener('fg-update', () => this.updateDisplay())
    this.updateDisplay()
  }

  updateDisplay() {
    const json = factGraph.toJson()
    const prettyJson = JSON.stringify(JSON.parse(json), null, 2)
    this.pre.innerText = prettyJson
  }
}
customElements.define('fg-json', FgJson)

