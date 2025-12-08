/**
 * Template paths for each form type
 */
const FORM_TEMPLATES = {
  w4: 'resources/w4-templates/fw4.pdf',
  w4sp: 'resources/w4-templates/fw4-es.pdf',
  w4p: 'resources/w4-templates/fw4p.pdf'
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
    credits: 'topmostSubform[0].Page1[0].f1_09[0]',
    nonJobIncome: 'topmostSubform[0].Page1[0].f1_10[0]',
    deductions: 'topmostSubform[0].Page1[0].f1_11[0]',
    extraWithholding: 'topmostSubform[0].Page1[0].f1_12[0]'
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
    pensionIncome: 'topmostSubform[0].Page1[0].f1_07[0]', // Only exists in W-4P
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
 */
async function downloadW4 (factGraph) {
  // Get all fact values from the Fact Graph
  const filingStatus = factGraph.get('/filingStatus').get.toString()                                // Line 1(c)
  const pensionIncome = factGraph.get('/totalPensionsIncome').get.toString()                        // Line 2(b)(i) amount (W-4P only)
  const credits = factGraph.get('/totalCredits').get.toString()                                     // Line 3 amount
  const nonJobIncome = factGraph.get('/totalNonJobsIncome').get.toString()                          // Line 4(a) amount
  const deductions = factGraph.get('/jobSelectedForExtraWithholding/w4Line4b').get.toString()       // Line 4(b) amount
  const extraWithholding = factGraph.get('/jobSelectedForExtraWithholding/w4Line4c').get.toString() // Line 4(c) amount

  // Determine which form template to use
  const hasPensionIncome = pensionIncome !== null && pensionIncome !== '' && pensionIncome > 0
  const isSpanish = document.querySelector('input[name="lang"]:checked')?.value === 'es'

  let template
  if (hasPensionIncome) {
    template = 'w4p'
  } else if (isSpanish) {
    template = 'w4sp'
  } else {
    template = 'w4'
  }

  // Load template and create form
  // eslint-disable-next-line security/detect-object-injection
  const templateResponse = await fetch(FORM_TEMPLATES[template])
  const templateBytes = await templateResponse.arrayBuffer()

  const { PDFDocument } = PDFLib
  const pdfDoc = await PDFDocument.load(templateBytes)
  const form = pdfDoc.getForm()

  const fields = template === 'w4p' ? FORM_FIELDS.w4p : FORM_FIELDS.w4

  // Set filing status checkbox
  // eslint-disable-next-line security/detect-object-injection
  form.getCheckBox(fields.filingStatus[filingStatus]).check()

  // Fill text fields
  form.getTextField(fields.credits).setText(String(credits))
  form.getTextField(fields.nonJobIncome).setText(String(nonJobIncome))
  form.getTextField(fields.deductions).setText(String(deductions))
  form.getTextField(fields.extraWithholding).setText(String(extraWithholding))
  if (fields.pensionIncome) form.getTextField(fields.pensionIncome).setText(String(pensionIncome))

  // Generate and download
  const pdfBytes = await pdfDoc.save()
  const url = URL.createObjectURL(new Blob([pdfBytes], { type: 'application/pdf' }))
  const link = Object.assign(document.createElement('a'), { href: url, download: `${template}.pdf` })
  link.click()
  URL.revokeObjectURL(url)
}

window.downloadW4 = downloadW4
