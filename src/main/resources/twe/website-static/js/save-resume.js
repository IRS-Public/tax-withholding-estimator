/**
 * Save & Resume for the Tax Withholding Estimator (taxpayer-facing).
 *
 * Lets a taxpayer download their in-progress estimate to a file and load it
 * back later, so closing the browser (which clears sessionStorage) doesn't lose
 * their work. The file is a small, versioned wrapper around the Fact Graph JSON
 * so it can be read by any future IRS tool that speaks the same format.
 *
 * Reuses the existing serialization already shipped for the audit panel:
 *   - window.factGraph.toJson()  (serialize current state)
 *   - window.loadFactGraph(str)  (replace state + reload)  [fg-components.js]
 *
 * No data leaves the device: the file is created client-side and read
 * client-side. Nothing is uploaded.
 */

import { openModal } from './modals.js'

const FORMAT = 'irs-twe-estimate'
const FORMAT_VERSION = 1
const FILE_NAME = 'twe-estimate.json'

document.getElementById('save-resume-button').addEventListener('click', () => {
  openModal(document.getElementById('modal-save-resume'))
})

/**
 * Serialize the current Fact Graph into the versioned interchange wrapper.
 * @returns {string} pretty-printed JSON
 */
function buildExportPayload () {
  const factGraph = window.factGraph.toJson()
  const payload = {
    format: FORMAT,
    version: FORMAT_VERSION,
    // savedAt is informational only; set from the client clock.
    savedAt: new Date().toISOString(),
    // factGraph is stored as a parsed object so the file is human-readable and
    // tool-friendly; it is re-stringified on import before loadFactGraph().
    factGraph: JSON.parse(factGraph)
  }
  return JSON.stringify(payload, null, 2)
}

/**
 * Download the current estimate as a file.
 */
function saveEstimateToFile () {
  const status = document.getElementById('save-estimate-status')
  try {
    const blob = new Blob([buildExportPayload()], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = FILE_NAME
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
    if (status) {
      status.textContent = status.dataset.success || 'Saved.'
      status.classList.add('animate-success')
      status.onanimationend = () => status.classList.remove('animate-success')
    }
  } catch (err) {
    console.error('Unable to save estimate to file.', err)
    if (status) status.textContent = status.dataset.error || 'Could not save your estimate. Please try again.'
  }
}
window.saveEstimateToFile = saveEstimateToFile

/**
 * Validate the wrapper and return the inner Fact Graph JSON string.
 * Accepts a bare Fact Graph too (forward/backward tolerance), but requires it
 * to at least parse as JSON.
 * @param {string} text raw file contents
 * @returns {string} fact graph JSON string ready for window.loadFactGraph
 */
function extractFactGraphJson (text) {
  const parsed = JSON.parse(text) // throws on invalid JSON -> caught by caller
  if (parsed && parsed.format === FORMAT && parsed.factGraph) {
    return JSON.stringify(parsed.factGraph)
  }
  // Tolerate a raw fact-graph object/string (e.g. copied from the audit panel).
  if (parsed && typeof parsed === 'object' && !parsed.format) {
    return JSON.stringify(parsed)
  }
  throw new Error('Unrecognized estimate file format')
}

/**
 * Read the chosen file and load it into the estimator after confirmation.
 * @param {Event} event change event from the file <input>
 */
function resumeEstimateFromFile (event) {
  const input = event.target
  const status = document.getElementById('resume-estimate-status')
  const file = input.files && input.files[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => {
    let factGraphJson
    try {
      factGraphJson = extractFactGraphJson(String(reader.result))
    } catch (err) {
      console.error('Invalid estimate file.', err)
      if (status) status.textContent = status.dataset.error || 'This file is not a valid saved estimate.'
      input.value = ''
      return
    }
    // Loading replaces the current estimate; confirm before discarding progress.
    const confirmMsg = input.dataset.confirm ||
      'Loading this file will replace your current estimate. Continue?'
    if (!window.confirm(confirmMsg)) {
      input.value = ''
      return
    }
    try {
      window.loadFactGraph(factGraphJson) // reloads the page on success
    } catch (err) {
      console.error('Unable to load estimate.', err)
      if (status) status.textContent = status.dataset.error || 'This file is not a valid saved estimate.'
      input.value = ''
    }
  }
  reader.onerror = () => {
    if (status) status.textContent = status.dataset.error || 'Could not read the file. Please try again.'
    input.value = ''
  }
  reader.readAsText(file)
}
window.resumeEstimateFromFile = resumeEstimateFromFile
