/**
 * Template paths for each form type
 */
const FORM_TEMPLATES = {
  w4: '/app/tax-withholding-estimator/resources/w4-templates/fw4.pdf',
  w4sp: '/app/tax-withholding-estimator/resources/w4-templates/fw4-es.pdf',
  w4p: '/app/tax-withholding-estimator/resources/w4-templates/fw4p.pdf'
}

/**
 * Maps tax filing statuses to W-4 form fields.
 * The tax code has 5 statuses, but the W-4 has 3 checkboxes.
 * Single or MFS -> Single. MFJ or QSS -> Married.
 */
const FORM_FIELDS = {
  // W-4 and W-4SP share field IDs
  w4: {
    filingStatus: {
      single: 'topmostSubform[0].Page1[0].c1_1[0]',
      marriedFilingJointly: 'topmostSubform[0].Page1[0].c1_1[1]',
      marriedFilingSeparately: 'topmostSubform[0].Page1[0].c1_1[0]', // Equivalent to Single
      headOfHousehold: 'topmostSubform[0].Page1[0].c1_1[2]',
      qualifiedSurvivingSpouse: 'topmostSubform[0].Page1[0].c1_1[1]' // Equivalent to Married
    },
    credits: 'topmostSubform[0].Page1[0].f1_08[0]',
    nonJobIncome: 'topmostSubform[0].Page1[0].f1_09[0]',
    deductions: 'topmostSubform[0].Page1[0].f1_10[0]',
    extraWithholding: 'topmostSubform[0].Page1[0].f1_11[0]'
  },
  // W-4P has different field IDs
  w4p: {
    filingStatus: {
      single: 'topmostSubform[0].Page1[0].c1_1[0]',
      marriedFilingJointly: 'topmostSubform[0].Page1[0].c1_1[1]',
      marriedFilingSeparately: 'topmostSubform[0].Page1[0].c1_1[0]', // Equivalent to Single
      headOfHousehold: 'topmostSubform[0].Page1[0].c1_1[2]',
      qualifiedSurvivingSpouse: 'topmostSubform[0].Page1[0].c1_1[1]' // Equivalent to Married
    },
    credits: 'topmostSubform[0].Page1[0].f1_12[0]',
    nonJobIncome: 'topmostSubform[0].Page1[0].f1_13[0]',
    deductions: 'topmostSubform[0].Page1[0].f1_14[0]',
    extraWithholding: 'topmostSubform[0].Page1[0].f1_15[0]'
  }
}

/**
 * Downloads W-4, W-4SP, or W-4P.
 * Form type decided by data: pensionIncome means W-4P. Otherwise language determines W-4 vs W-4SP.
 * @param {Object} factGraph - Fact graph instance
 * @param {String} jobId - Collection Id of the job for which to generate a W-4
 */
async function downloadW4 (factGraph, jobId) {
  // Get all fact values from the Fact Graph
  const shouldLeaveW4Line3Blank = factGraph.get(`/jobs/#${jobId}/shouldLeaveW4Line3Blank`).get
  const shouldLeaveW4Line4aBlank = factGraph.get(`/jobs/#${jobId}/shouldLeaveW4Line4aBlank`).get
  const shouldLeaveW4Line4bBlank = factGraph.get(`/jobs/#${jobId}/shouldLeaveW4Line4bBlank`).get
  const shouldLeaveW4Line4cBlank = factGraph.get(`/jobs/#${jobId}/shouldLeaveW4Line4cBlank`).get

  // -- Line 1(c)
  const filingStatus = factGraph.get('/filingStatus').get.toString()
  // -- Line 3 amount
  const credits = shouldLeaveW4Line3Blank ? '' : factGraph.get(`/jobs/#${jobId}/w4Line3`).get.toString()
  // -- Line 4(a) amount
  const nonJobIncome = shouldLeaveW4Line4aBlank ? '' : factGraph.get(`/jobs/#${jobId}/w4Line4a`).get.toString()
  // -- Line 4(b) amount
  const deductions = shouldLeaveW4Line4bBlank ? '' : factGraph.get(`/jobs/#${jobId}/w4Line4b`).get.toString()
  // -- Line 4(c) amount
  const extraWithholding = shouldLeaveW4Line4cBlank ? '' : factGraph.get(`/jobs/#${jobId}/w4Line4c`).get.toString()

  const values = {
    filingStatus,
    credits,
    nonJobIncome,
    deductions,
    extraWithholding
  }

  await downloadPdf(
    FORM_TEMPLATES.w4,
    FORM_FIELDS.w4,
    values,
    jobId,
    `w-4-${jobId}`)
}
window.downloadW4 = downloadW4

async function downloadW4P (factGraph, pensionId) {
  // Get all fact values from the Fact Graph
  const shouldLeaveW4pLine3Blank = factGraph.get(`/pensions/#${pensionId}/shouldLeaveW4pLine3Blank`).get
  const shouldLeaveW4pLine4aBlank = factGraph.get(`/pensions/#${pensionId}/shouldLeaveW4pLine4aBlank`).get
  const shouldLeaveW4pLine4bBlank = factGraph.get(`/pensions/#${pensionId}/shouldLeaveW4pLine4bBlank`).get
  const shouldLeaveW4pLine4cBlank = factGraph.get(`/pensions/#${pensionId}/shouldLeaveW4pLine4cBlank`).get

  // -- Line 1(c)
  const filingStatus = factGraph.get('/filingStatus').get.toString()
  // -- Line 3 amount
  const credits = shouldLeaveW4pLine3Blank ? '' : factGraph.get(`/pensions/#${pensionId}/w4pLine3`).get.toString()
  // -- Line 4(a) amount
  const nonJobIncome = shouldLeaveW4pLine4aBlank ? '' : factGraph.get(`/pensions/#${pensionId}/w4pLine4a`).get.toString()
  // -- Line 4(b) amount
  const deductions = shouldLeaveW4pLine4bBlank ? '' : factGraph.get(`/pensions/#${pensionId}/w4pLine4b`).get.toString()
  // -- Line 4(c) amount
  const extraWithholding = shouldLeaveW4pLine4cBlank ? '' : factGraph.get(`/pensions/#${pensionId}/w4pLine4c`).get.toString()

  const values = {
    filingStatus,
    credits,
    nonJobIncome,
    deductions,
    extraWithholding
  }

  await downloadPdf(
    FORM_TEMPLATES.w4p,
    FORM_FIELDS.w4p,
    values,
    pensionId,
    `w-4p-${pensionId}`)
}
window.downloadW4P = downloadW4P

async function downloadPdf (template, fields, values, collectionId, fileName) {
  // Load template and create form
  const templateResponse = await fetch(template) // TODO: Use spanish template when the app is used in spanish
  const templateBytes = await templateResponse.arrayBuffer()

  const { PDFDocument } = PDFLib
  const pdfDoc = await PDFDocument.load(templateBytes)
  const form = pdfDoc.getForm()

  // Set filing status checkbox
  form.getCheckBox(fields.filingStatus[values.filingStatus]).check()

  // Fill text fields
  form.getTextField(fields.credits).setText(String(values.credits))
  form.getTextField(fields.nonJobIncome).setText(String(values.nonJobIncome))
  form.getTextField(fields.deductions).setText(String(values.deductions))
  form.getTextField(fields.extraWithholding).setText(String(values.extraWithholding))

  // Generate and download
  const pdfBytes = await pdfDoc.save()
  const url = URL.createObjectURL(new Blob([pdfBytes], { type: 'application/pdf' }))
  const link = Object.assign(
    document.createElement('a'), { href: url, download: `${fileName}.pdf` }
  )
  link.click()
  URL.revokeObjectURL(url)
}
