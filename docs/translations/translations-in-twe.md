# Translations in TWE

## Overview

The current supported languages are **English** (`en`) and **Spanish** (`es`).

A high-level view of the translation flow:

* English content is edited directly in the app's XML files `src/main/resources/twe/flow`
* `flow_en.yaml` is generated from the XML flow files
* `flow_es.yaml` is manually updated to match
* `en.yaml` and `es.yaml` are both manually updated, and contain non-flow content (text for shared components, layout, errors, etc.)

Translations are published in `out/` by running `make`, generating a separate, fully translated version of XML (and consequently a separate build of the static site) for each supported language, using that language's corresponding yaml files, e.g. `es.yaml` and `flow_es.yaml`.

## Creating and Updating Content
User-facing English-language content lives in two places:

1. **Directly in the XML files.** All content for the flow pages (`src/main/resources/twe/flow`) is updated in these XML files. Run `make` to generate a new `flow_en.yaml` to reflect any updates. `flow_es.yaml` updates must be applied manually.

2. <strong>In `en.yaml` and `es.yaml`.</strong> These files contain non-flow, shared app content, such as buttons, form values, layout copy, and errors. They were manually created for both English `en.yaml` and Spanish `es.yaml`. Each file must be manually updated.

### Example: Income Flow Page
All UI copy for a given flow page lives in its corresponding Flow XML file under `src/main/resources/twe/flow/`.

For example, `income.xml` contains all of the questions, labels, hints, and modal dialog content for the Income & Tax Payments page (`/app/tax-withholding-estimator/income/`).

Text is written directly as XML content, and `flow_en.yaml` is automatically generated. Any updates made to `flow_en.yaml` need to be applied manually to `flow_es.yaml`, matching the appropriate keys.

**Example:** A question in `income.xml`:

```xml
<fg-set path="/jobs/*/overtimeCompensationRate">
  <question>Overtime pay rate</question>
  <hint>Leave this blank if you don't have a Social Security number.</hint>
  <option value="onePointFive">1.5 times my regular pay</option>
  <option value="two">2.0 times my regular pay</option>
</fg-set>
```

This generated entries in the `flow_en.yaml` file:

```yaml
      /jobs/*/overtimeCompensationRate:
        question: What is the rate at which you get paid overtime?
        modalLink: <modal-link for="modal-estimate-overtime-rate-en">What if I get
          paid a different overtime rate?</modal-link>
        options:
          onePointFive:
            name: 1.5 times my regular pay
          two:
            name: 2.0 times my regular pay

```

Matching entries (which were manually added) exist in `flow_es.yaml`:

```yaml
      /jobs/*/overtimeCompensationRate:
        question: ¿Cuánto te pagan por horas extra?
        modalLink: <modal-link for="modal-estimate-overtime-rate-en">¿Qué pasa si me pagan una tarifa por horas extra?</modal-link>
        options:
          onePointFive:
            name: 1.5 veces mi paga regular
          two:
            name: 2.0 veces mi paga regular
```

Using the above `income.xml` example, let's make a test update to the content.

### Update 1: Change existing text
Let's change `2.0` to `2`.

1. In `income.xml`, search for `/jobs/*/overtimeCompensationRate` to locate the content.
2. Update the copy to match desired change.
```xml
  <option value="two">2 times my regular pay</option>
```
3. Save file and run `make`. Open `flow_en.yaml`, search for the key `/jobs/*/overtimeCompensationRate` and confirm your change is reflected.
4. Open `flow_es.yaml` and search for the same key, `/jobs/*/overtimeCompensationRate`. Update the copy to align with changes in `flow_en.yaml`. Save. If the app is already running, it will recompile. Otherwise, run `make`.
5. Navigate to `/app/tax-withholding-estimator/income/` and confirm the change in the UI.


### Update 2: Change existing text of hashed element
Some yaml keys contain a hash (the first 6 characters of an MD5 string). This prevents duplicate keys, for example when there are siblings with the same tag and no ID. This approach was chosen over adding incremented indices to keys (e.g. `p-1:`, `p-2:`, etc.) because it also limits the required changes in the event content is deleted or moved.

Because `flow_en.yaml` is generated, updating an element that outputs to a hashed key will also update the hash. This means we need to manually change the hash in `flow_es.yaml`.

Continuing with `income.xml`, let's make a test update to a hashed element.

* Find the element in `income.xml` to update.
```xml
      <h2><fg-show path="/taxYear"/> Income &amp; tax payments</h2>
```

* Open `flow_en.yaml` and find the corresponding key. Note the hash, `fdf8c2`.

```yaml
/income:
  title: Income & tax payments
  h2-fdf8c2: <fg-show path="/taxYear"/> Income &amp; tax payments
```

* Change text and save
```xml
      <h2><fg-show path="/taxYear"/> Income</h2>
```

* Open `flow_en.yaml` and find/confirm the change. Note the new hash, `5ceb9f`.

```yaml
/income:
  title: Income & tax payments
  h2-5ceb9f: <fg-show path="/taxYear"/> Income
```

* Open `flow_es.yaml` and find the key, which will use the old hash.
```yaml
h2-fdf8c2: Ingresos y pagos de impuestos del <fg-show path="/taxYear"/>
```

* Update the text with the new copy, _and_ update the hash to match.
```yaml
h2-5ceb9f: Ingresos del <fg-show path="/taxYear"/>

```


### Update 3: Add new text element
Because `flow_es.yaml` is manually updated, you'll need to manually add a new entry if adding a text element in the flow files creates a new entry in `flow_en.yaml`.

Similar to both examples above, you'd:

* Make change in `income.xml`
* Find and copy new key/value in newly generated `flow_en.yaml`
* Update `flow_es.yaml` with new key/value. To keep things organized, add new content in the same location in the file as it's added in `flow_en.yaml`.


## Setting up a new language
Let's use Spanish as the example. (Note: These files already exist! Don't overwrite them.)

What we need to do:
* Generate `flow_es.yaml` to manage flow content
* Manually create `es.yaml` to manage non-flow content
* Update code to build and output to `/es/` subdirectory

### Generate `flow_es.yaml`
Since `flow_es.yaml` will be manually updated, we need to generate an empty file with identical keys to the original flow file, `flow_en.yaml`.

* Run the app with `make`
* Open the file `Locale.scala` in your IDE, and search for `anyEncoder`:
```scala
implicit val anyEncoder: Encoder[Any] = Encoder.instance {
  case m: mutable.LinkedHashMap[_, _] => Json.obj(m.map { case (k, v) => (k.toString, anyEncoder(v)) }.toSeq*)
  case s: String                      => Json.fromString(s)
}
```

Usually, this piece of code copies the string values from the linked hash map generated from the XML files in `TranslationContext`. We're going to use it to instead output the values as empty strings.

* Change `Json.fromString(s)` to `Json.fromString("")`
* With `make` running, the file `flow_en.yaml` should regenerate with only empty strings as values for the keys.
* Open `flow_en.yaml`, confirm there are no values, and save a copy as your new file, e.g. `flow_es.yaml`
* Find/replace empty strings `''` to remove them.

Don't forget to restore your original `flow_en.yaml` file.
* Revert the change in `Locale.scala` to `Json.fromString(s)`
* Run `make` to generate a new `flow_en.yaml` file.

### Manually create `es.yaml`
Make a copy of `en.yaml` and save it as `es.yaml`. You'll need to either remove existing content or overwrite it as translations are added.

### Update code to output to subdirectory
Any language other than English gets its own subdirectory in `out` when the website is generated. <strong>Note:</strong> Both yaml files above must be generated before this update is made.
* Open `Website.scala` and locate the list of locales. Continuing with our example of how Spanish was added, assume it looks like this:
```scala
    val locales = List("en")
```

* Add the new language code to the list: `List("en", "es")`

* Run `make`. You should see a new subdirectory in `out` called `es`.

## Tests
What tests in `YamlValidatorSpec.scala` check:
* Yaml Keys are unique
* Identical keys are present in both `en.yaml` and `es.yaml`
* Identical keys are present in both `flow_en.yaml` and `flow_es.yaml`
* If a tag + condition are used as part of a value in one language's yaml file, it exists in the other language's corresponding file.
  e.g.
  ```yaml
  // flow_en.yaml
  /jobs/*/writableEndDate:
      question: |-
        What’s the last day in <fg-show path="/taxYear"/>
                    <span condition="/jobs/*/isFilerAssignmentSelf" operator="isFalse">they</span>
                    <span condition="/jobs/*/isFilerAssignmentSelf" operator="isTrue">your</span> expect to have this job?


  ```
  In this example, `<span condition="/jobs/*/isFilerAssignmentSelf"` should also be used in the corresponding value in `flow_es.yaml`.

  ```yaml
  // flow_es.yaml
    question: |-
      ¿Cuál es el último día del <fg-show path="/taxYear"/> que
      <span condition="/jobs/*/isFilerAssignmentSelf" operator="isFalse">espera</span>
      <span condition="/jobs/*/isFilerAssignmentSelf" operator="isTrue">esperas</span>
      <span condition="/jobs/*/isFilerAssignmentSelf" operator="isIncomplete">esperas</span> tener este trabajo?

  ```

  **Q: Is it valid that the Spanish version has an additional span with an operator that isn't used in the English?**

  **A: Yes.** The test is intentionally operator-agnostic and focuses only on the presence of the tag and condition (fact). In another language, it might require multiple elements with the same condition and the same or different operator to communicate the same content. See `normalizeForValidation` in `YamlValidatorSpec.scala` for the full code.

  There may be translations where language differences do require conditional tags in one language, but not another. For these special cases, you can skip the validation by ignoring specific translation keys inside `YamlValidatorSpec.scala` under `ignoredTranslationKeys`.



