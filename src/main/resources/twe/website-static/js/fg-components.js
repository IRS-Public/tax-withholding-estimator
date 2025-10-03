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
 * Debug utility to load a fact graph from the console
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
  constructor() {
    super()

    this.tabListener = (event) => {
      // Conditions must be re-evaluated before the keydown event resolves, so that focusable elements are updated
      // before the focus moves. The `blur` and `change` events don't fire until *after* the focus has already moved.
      if(event.key === 'Tab') {
        // TODO: Prevent these from being called twice (once here, once through onChange)
        this.setFact()
        showOrHideAllElements()
      }
    }
  }

  connectedCallback() {
    this.condition = this.getAttribute('condition')
    this.operator = this.getAttribute('operator')
    this.inputType = this.getAttribute('inputtype')
    this.inputs = this.querySelectorAll('input, select')
    this.optional = this.getAttribute('optional') === 'true'

    switch (this.inputType) {
      // This switch statement is intentionally not exhaustive
      case 'date': {
        this.addEventListener('change', () => {
          const allFilled = Array.from(this.inputs).every(input => {
            return input.value.trim() !== '' && input.value !== '- Select -';
          });

          if (allFilled) {
            this.onChange();
            this.validateRequiredFields();
          }
        });

        break
      }
      case 'dollar':
        this.addEventListener('change', () => {
            this.onChange()
            // Clear validation error once user provides valid input
            this.validateRequiredFields();
          });
        break;
      case 'select':
      case 'boolean':
      case 'enum':
        for (const input of this.inputs) {
          input.addEventListener('change', () => {
            this.onChange()
            // Clear validation error once user makes a selection
            this.clearValidationError();
          });
        }
        break;
      default:
        for (const input of this.inputs) {
          input.addEventListener('blur', () => this.onChange());
          input.addEventListener(`keydown`, this.tabListener)
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

  clearAlerts() {
    this.querySelector('div.alert--warning')?.remove()
  }

  clearValidationError() {
    const errorElement = this.querySelector('.usa-error-message');
    const errorId = errorElement?.id;

    // Remove errorId from aria-describedby
    const elementWithDescription = this.querySelector('[aria-describedby]');
    const ariaDescription = elementWithDescription?.getAttribute('aria-describedby');

    if (elementWithDescription) {
    const updatedIds = ariaDescription
      .split(' ')
      .filter(id => id.trim() && id !== errorId)
      .join(' ');

    updatedIds
      ? elementWithDescription.setAttribute('aria-describedby', updatedIds)
      : elementWithDescription.removeAttribute('aria-describedby');
    }

    //Remove the error treatment
    errorElement?.remove()
    this.querySelector('.validate-alert')?.remove()
    this.querySelector('.usa-form-group')?.classList.remove('usa-form-group--error')
    this.querySelector('.usa-label--error')?.classList.remove('usa-label--error')
    this.querySelectorAll('.usa-input-group, .usa-select, .usa-input').forEach(item => {
      item.classList.remove('usa-input--error');
      item.removeAttribute('aria-describedby');
    });
    this.querySelectorAll('.usa-input[aria-invalid="true"], .usa-select[aria-invalid="true"]').forEach(item => item?.setAttribute('aria-invalid', 'false'));

  }

  setValidationError() {
    this.clearValidationError();
    const errorId = `${this.path}-error`; // Keep the slash for primary filer

    // Set up the error div
    const errorDiv = document.createElement('div');
    errorDiv.classList.add('usa-error-message');
    errorDiv.setAttribute('id', errorId);
    errorDiv.textContent = "this field is required";

    const elementWithDescription = this.querySelector('.usa-fieldset, .usa-select, .usa-input');
    const errorLocation = this.querySelector('.usa-radio, .usa-memorable-date, .usa-checkbox, .usa-select, .usa-input-group, .usa-input');

    // Place the error div just before the invalid field location
    errorLocation.insertAdjacentElement('beforebegin', errorDiv);

    // Set aria-description
    const existingAriaDescribedby = elementWithDescription.getAttribute('aria-describedby');
    elementWithDescription.setAttribute('aria-describedby', `${existingAriaDescribedby || ''} ${errorId}`.trim());

    // Set the modifier classes for errors
    this.querySelector('.usa-form-group')?.classList.add('usa-form-group--error');
    this.querySelector('.usa-legend, .usa-label')?.classList.add('usa-label--error');
    this.querySelectorAll('.usa-input-group, .usa-select, .usa-input').forEach(item => {
      item.classList.add('usa-input--error');
      item.setAttribute('aria-describedby', `${errorId}`);
    });
    this.querySelectorAll('.usa-input[aria-invalid="false"], .usa-select[aria-invalid="false"]').forEach(item => {
      item.setAttribute('aria-invalid', 'true');
    });
  }

  validateRequiredFields() {
    const isMissing = !this.isComplete();
    if (isMissing) {
        this.setValidationError();
    } else {
        this.clearValidationError();
    }
    return isMissing;
  }

  render() {
    this.clearAlerts()

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
      case 'boolean':
      case 'enum': {
        const checkedRadio = this.querySelector(`input:checked`)
        if (checkedRadio) {
          checkedRadio.checked = false;
        };
        break
      }
      case 'select': {
        this.querySelector('select').value = ''
        break
      }
      case 'text':
      case 'date': {
        this.querySelector('select[name*="-month"]').value = '';
        this.querySelector('input[name*="-day"]').value = '';
        this.querySelector('input[name*="-year"]').value = '';
      }
      case 'int':
      case 'dollar': {
        this.querySelector('input').value = ''
        break
      }
      default: {
        console.warn(`Unknown input type "${this.inputType}" for input with path "${this.path}"`)
      }
    }

    // Clear error and alerts
    this.error = null;
    this.clearAlerts()
    this.clearValidationError()
  }

  setInputValueFromFactValue() {
    console.debug(`Setting input value for ${this.path} of type ${this.inputType}`)
    let fact = factGraph.get(this.path)

    let value
    if (fact.complete === false) {
      value = ""
    } else {
      value = fact.get?.toString()
    }

    switch (this.inputType) {
      case 'boolean':
      case 'enum': {
        if (value !== "") {
          this.querySelector(`input[value=${value}]`).checked = true
        }
        break;
      }
      case 'select': {
        this.querySelector('select').value = value
        break
      }
      case 'text':
      case 'int':
      case 'date': {
        const dateString = value;
        if (dateString) {
          // Fact has a complete date value - set all fields
          const [year, month, day] = dateString.split('-');
          this.querySelector('select[name*="-month"]').value = month;
          this.querySelector('input[name*="-day"]').value = day;
          this.querySelector('input[name*="-year"]').value = year;
        } else if (this.inputType === 'date') {
          // For incomplete date facts, preserve existing input values
          // Only clear if the inputs are truly empty (not just fact incomplete)
          const monthSelect = this.querySelector('select[name*="-month"]');
          const dayInput = this.querySelector('input[name*="-day"]');
          const yearInput = this.querySelector('input[name*="-year"]');

          // Only reset to empty if there are no current values
          // This preserves partial user input during fg-update events
          if (!monthSelect.value && !dayInput.value && !yearInput.value) {
            monthSelect.value = '';
            dayInput.value = '';
            yearInput.value = '';
          }
          // If there are existing values, leave them alone
        } else {
          // For non-date inputs, clear when fact has no value
          this.querySelector('input').value = '';
        }
        break
      }
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
    console.debug(`Getting input value for ${this.path} of type ${this.inputType}`);
    switch (this.inputType) {
      case 'boolean':
      case 'enum': {
        return this.querySelector('input:checked')?.value
      }
      case 'select': {
        return this.querySelector('select')?.value
      }
      case 'text':
      case 'date': {
        const month = this.querySelector('select[name*="-month"]')?.value;
        const day = this.querySelector('input[name*="-day"]')?.value;
        const year = this.querySelector('input[name*="-year"]')?.value;
        // Adding padStart to day changes user's input from 1 to 01
        const dateString = `${year}-${month}-${day.padStart(2, '0')}`;
        return dateString;
      }
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
      case 'boolean':
      case 'enum': {
        const input = this.querySelector('input:checked')
        if (input) input.checked = false
        break;
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
  constructor() {
    super()

    // Make listener a persistent function so we can remove it later
    this.addItemListener = () => this.addItem();

    /*
    * Set item numbers for items in `<fg-collection>`
    * Changes headings to Item 1, Item 2, etc.
    */

    this.setCollectionItemNumbers = () => {
      const collectionItems = this.querySelectorAll('fg-collection-item');
      collectionItems.forEach((item, index) => {
        const itemNumberSlot = item.querySelectorAll('.collection-item-number');
        if (itemNumberSlot) {
          itemNumberSlot.forEach(slot => slot.textContent = `${index + 1}`);
        }
      });
    };
  }

  connectedCallback() {
    this.path = this.getAttribute('path')
    this.addItemButton = this.querySelector('.fg-collection__add-item')
    this.addItemButton.addEventListener('click', this.addItemListener)

    // Add any items that currently exist in this collection
    const ids = factGraph.getCollectionIds(this.path)
    ids.map(id => this.addItem(id))
  }

  disconnectedCallback() {
    this.addItemButton.removeEventListener('click', this.addItemListener)
  }

  addItem(id) {
    const collectionId = id ?? crypto.randomUUID()

    if (!id) {
      factGraph.addToCollection(this.path, collectionId)
      saveFactGraph()
    }

    const collectionItem = document.createElement('fg-collection-item')
    collectionItem.setAttribute(`collectionPath`, this.path)
    collectionItem.setAttribute(`collectionId`, collectionId)
    const collectionItemsContainer = this.querySelector('.usa-accordion')
    collectionItemsContainer.appendChild(collectionItem)
    const collectionAccordionButton = collectionItem.querySelector('.usa-accordion__button');
    collectionAccordionButton?.focus();
    document.dispatchEvent(new CustomEvent('fg-update'))
  }

}
customElements.define('fg-collection', FgCollection)

class FgCollectionItem extends HTMLElement {
  constructor() {
    super()

    // Make listener a persistent function so we can remove it later
    this.clearListener = () => this.clear()
    this.removeItemListener = () => {
      const fgCollection = this.closest('fg-collection')
      const addButton = fgCollection.querySelector('.fg-collection__add-item')
      this.clear();
      addButton.focus();
      this.dispatchEvent(new CustomEvent('fg-update'));
    }
  }

  connectedCallback() {
    console.debug('Connecting', this)

    // Get our template from the parent fg-collection
    const fgCollection = this.closest('fg-collection')
    const addButton = fgCollection.querySelector('.fg-collection__add-item')
    const templateContent = fgCollection.querySelector('.fg-collection__item-template').content.cloneNode(true)

    // Update all abstract paths in the template to include the collection id
    const collectionId = this.getAttribute('collectionId');

    const attributes = ['path', 'condition', 'id', 'for', 'name', 'aria-describedby']
    const nodesWithAbstractPaths = templateContent.querySelectorAll(attributes.map(attr => `[${attr}*="/*/"]`).join(','))
    for (const node of nodesWithAbstractPaths) {
      for (const attribute of attributes) {
        const path = node.getAttribute(attribute)
        if (path) {
          node.setAttribute(attribute, makeCollectionIdPath(path, collectionId))
        }
      }
    }

    this.append(templateContent);

    // Set up accordion ids to enable interactions
    const collectionAccordionButton = this.querySelector('.usa-accordion__button');
    const collectionItemContent = this.querySelector('.usa-accordion__content');
    collectionAccordionButton.setAttribute('aria-controls', `collection-item-${collectionId}`);
    collectionItemContent.setAttribute('id', `collection-item-${collectionId}`);

    this.removeButton = this.querySelector('.fg-collection-item__remove-item')
    this.removeButton.addEventListener('click', this.removeItemListener)

    document.addEventListener('fg-clear', this.clearListener)

    // Set collection item numbers
    fgCollection.setCollectionItemNumbers()
  }

  disconnectedCallback() {
    console.debug('Disconnecting', this)

    this.removeButton.removeEventListener('click', this.removeItemListener)
    document.removeEventListener('fg-clear', this.clearListener)

    // Reset content
    this.innerHTML = ''
  }

  clear() {
    for (const fgSet of this.querySelectorAll(customElements.getName(FgSet))) {
      fgSet.remove();
    }

    const fgCollection = this.closest('fg-collection')
    const collectionPath = this.getAttribute(`collectionPath`)
    const collectionId = this.getAttribute(`collectionId`)
    factGraph.delete(makeCollectionIdPath(`${collectionPath}/*`, collectionId));
    saveFactGraph()

    // Remove this element and its parent fieldset from the DOM after removing the item from the fact graph
    this.remove();
    fgCollection.setCollectionItemNumbers()
  }
}
customElements.define('fg-collection-item', FgCollectionItem)

/*
 * <fg-show> - Display the current value and/or status of a fact.
 */
class FgShow extends HTMLElement {
  constructor() {
    super()

    this.updateListener = () => this.render()
  }

  connectedCallback() {
    this.path = this.getAttribute('path')
    document.addEventListener('fg-update', this.updateListener)
    this.render()
  }

  disconnectedCallback() {
    document.removeEventListener('fg-update', this.updateListener)
  }

  render() {
    // TODO: Eventually remove as part of https://github.com/IRSDigitalService/tax-withholding-estimator/issues/414
    // This is a temporary enhancement to allow showing all values of a fact without knowing the collection id
    const results = (this.path.indexOf('*') !== -1)
      ? factGraph.getVect(this.path).Lgov_irs_factgraph_monads_MaybeVector$Multiple__f_vect.sci_Vector__f_prefix1.u
      : [factGraph.get(this.path)]

    let outputHtml = ''
    for (const result of results) {
      if (outputHtml !== '') outputHtml += ', '
      console.log(result.hasValue)
      if (result.hasValue) {
        const value = result.get.toString()
        if (result.complete === false) {
          outputHtml += `${value}&nbsp;<span class="text-base-light">[Placeholder value]</span>`
        } else {
          outputHtml += `${value}`
        }
      } else {
        outputHtml += `<span class="text-base">-</span>`
      }
    }
    this.innerHTML = outputHtml
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
      // Only delete facts for <fg-set>, not other elements that might have conditions
      if (element.tagName === 'FG-SET'){
        element?.deleteFactNoUpdate()
      }
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
  let hasValidationErrors = false;

  // Loop through fields and mark incomplete if empty and required
  for (const fgSet of fgSets) {
    if (!(fgSet.isComplete() || fgSet.optional)) {
      const fieldName = fgSet.path;
      missingFields.push(fieldName);
      if (!fgSet.validateRequiredFields()) {
        hasValidationErrors = false;
      }
    }
  }
  // Display validation error if there are missing fields/incomplete
  if (missingFields.length > 0 || hasValidationErrors) {
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

  // Focus the first invalid field
  const firstInvalidField = document.querySelector('fg-set:not([inputtype="date"], .hidden) .usa-form-group--error .usa-fieldset, fg-set:not(.hidden) [aria-invalid="true"]');

  if (firstInvalidField instanceof HTMLFieldSetElement) {
    firstInvalidField.setAttribute('tabindex', '-1');
    firstInvalidField.focus();

    // Remove tabindex after focus to prevent outline from appearing on subsequent clicks
    firstInvalidField.addEventListener('blur', () => {
      firstInvalidField.removeAttribute('tabindex');
    }, { once: true });
  }
  else {firstInvalidField.focus();}

}

window.handleSectionContinue = handleSectionContinue;
window.handleSectionComplete = handleSectionComplete;

// Add show/hide functionality to all elements
document.addEventListener('fg-update', showOrHideAllElements)
showOrHideAllElements()
