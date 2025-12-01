# Tax Withholding Basics

> [!WARNING]
> Nothing in this document should be construed as tax guidance or official tax filing instructions.
> This is a high-level overview to explain how the online tax withholding estimator works and
> the information included here is not guaranteed to be up-to-date or correct.
> Do not calculate your taxes based on this document.
> See the Legal Notice in the README for more.

Per [Pub505](https://www.irs.gov/publications/p505), there are two ways to pay as you go:

1. Withholding - under most circumstances, if you are an employee whose paycheck is processed by a payroll provider, certain taxes are paid on your earnings throughout the year by your employer and/or the payroll provider.
If you have earnings that your employer is unaware of, you can instruct them to withhold additional amounts, which are paid to the IRS in your name.
2. Estimated tax - you can make quarterly estimated payments on your income directly to the IRS.
This is typically how you pay taxes if you are self-employed or derive significant income via other means such as capital gains, rents, and dividends.

When you start a job, you submit a W-4 to the employer that provides the information they need to withhold the appropriate amount from your paycheck.

The W-4 is a [relatively simple, four-page form](https://www.irs.gov/pub/irs-pdf/fw4.pdf), consisting of one page that the taxpayer submits to the employer, and three pages of information and supplemental worksheets.
Broadly speaking, the paper W-4 is capable of estimating the taxpayer's withholdings if they (or they and their spouse) have three or fewer jobs, have no other sources of income, and do not change jobs during the year.
Many taxpayers fall within this category, but many do not.
For the ones that do not, the paper W-4 repeatedly directs them to use the Tax Withholding Estimator (TWE) found at [https://www.irs.gov/individuals/tax-withholding-estimator](https://www.irs.gov/individuals/tax-withholding-estimator).

It is TWE's job to provide W-4s for taxpayers with varying levels of tax scenario complexity, including those that fall outside the scope of the attached worksheets.
Taxpayers answer questions and TWE generates a printable W-4 with the correct amount of additional withholdings.
TWE also estimates what the taxpayer will owe/be owed at the end of the year if they make no changes to their current withholding strategy.

## Calculating The Tax Gap

In essence, TWE needs to figure out the tax gap, which is the difference between the total the taxpayer will owe at the end of the year and the withholdings and estimated taxes that have already been paid to the IRS.

```math
\text{taxGap} = \text{totalOwed} - \text{totalCommittedWithholding}
```

By adjusting their withholdings, the taxpayer will ideally end the year with a tax gap of zero, where they owe nothing at the end of the year, or slightly positive, where they have overpaid and will receive a refund when filing their annual return.
TWE generates a W-4 that will apply additional withholding (or, if you have negative tax gap, reduced withholding) to make that happen.


```math
0 = \text{totalOwed} - \text{totalCommittedWithholding} - \text{additionalWithholding}
```

> [!TIP]
> An individual's tax gap is not to be confused with the [gross tax gap](https://www.irs.gov/statistics/irs-the-tax-gap): the difference between the true tax liability *for the entire country* for a given tax year and the amount that is paid on time.


### Total Owed

Calculating the first term, $\text{totalOwed}$, is reasonably straightforward.
Taxes owed are determined by filling out a [1040](https://www.irs.gov/pub/irs-pdf/f1040.pdf) with the income that the taxpayer expects to have at the end of the year, as well as the adjustments, deductions, and credits that the taxpayer indicates they are eligible for.
Although filling out the paper 1040 can be tedious to do by hand, it is a relatively uncomplicated task for a computer program, since there is a single, definitive answer for how much the taxpayer will owe if their income doesn't change for the rest of the year.


In simple terms, to calculate taxable income, one first calculates the Adjusted Gross Income (AGI).

```math
\text{agi} = \text{totalIncome} - \text{adjustments}
```

Deductions are then subtracted from the AGI to arrive at taxable income.
AGI is also used to determine eligibility for various credits.


```math
\text{taxableIncome} = \text{agi} - \text{deductions}
```

That $\text{taxableIncome}$ value is then used to calculate $\text{tentativeTax}$, via the [tax table](https://www.irs.gov/instructions/i1040tt#idm139749562274528) indicated by the [Line 16 instructions](https://www.irs.gov/pub/irs-pdf/i1040gi.pdf).

From there, additional taxes (like self-employment tax, if necessary) might be added, as well as non-refundable credits, to arrive at the $\text{totalTax}$ value.
This is line 24 of the 1040.

Finally, refundable tax credits and any advance estimated payments are subtracted from the $\text{totalTax}$ value.

> [!TIP]
> Refundable credits are applied last because the taxpayer can get them as a refund even if they don't owe any tax.
> In other words, $\text{totalOwed}$ can be negative, but $\text{totalTax}$ cannot.

```math
\text{totalOwed} = \text{totalTax} - \text{totalEstimatedTaxesPaid} - \text{totalRefundableCredits}
```

Altogether, this is essentially what the 1040 calculates: the total amount of money that you owe to, or are owed by, the US Government for one tax year.

### Total Committed Withholding

The second term required to calculate the $\text{taxGap}$ is $\text{totalCommittedWithholding}$.
This is where the bulk of tax estimation complexity lies.

The $\text{totalCommittedWithholding}$ is the sum of what has been withheld and what is expected to be withheld from the paychecks of the taxpayer's $n$ jobs.

```math
\text{totalCommittedWithholding} = \displaystyle\sum_{i=1}^{n} \text{committedWithholding}(j_i)
```

So if the taxpayer has three jobs, $j_1$, $j_2$, and $j_3$, then the $\text{totalCommittedWithholding}$ is the committed withholdings from each of those three jobs.

```math
  \displaystyle\sum_{i=1}^{3} \text{committedWithholding}(j_i) =
    \text{committedWithholding}(j_1) +
    \text{committedWithholding}(j_2) +
    \text{committedWithholding}(j_3)
```

It's called the "committed" withholding because it's money that has either already been withheld from this job's paycheck, or is expected to be paid as part of the standard withholding calculation.
For a single job $j$, the committed withholding is derived as follows:

```math
\text{committedWithholding}(j) =
  \text{pastWithholding}(j) +
  \text{lagWithholding}(j) +
  \text{futureTentativeWithholding}(j) +
  \text{bonusWithholding}(j)
```

Let's break down each of these terms one-by-one.

$\text{pastWithholding}(j)$ is simply the amount that has already been withheld from the taxpayer's paychecks for this job, this year, so far.
The taxpayer provides this value directly.

$\text{lagWithholding}(j)$ is what TWE expects the new employer who receives the W-4 generated by TWE will withhold after the W-4 is submitted, but before the withholding adjustments specified in this W-4 are applied, i.e. before the changes are reflected on the taxpayer's paycheck.
This is a **major assumption**.
Effectively, TWE guesses that it will take about 2 weeks from the time the taxpayer fills out the estimator to when the employer will update their payroll with the new values derived from the W-4.

$\text{futureTentativeWithholding}(j)$ is an estimate of what employer will withhold from the taxpayer's paycheck, based on the values the taxpayer fills out the W-4 with, *not including any additional withholdings*.
Think of this as the "standard withholding," an estimate of what would be appropriate to withhold for this particular job, based on coarse-grain information such as the taxpayer's filing status and number of dependents.
This is step 2h on Worksheet 1A of the [Pub15-T](https://www.irs.gov/pub/irs-pdf/p15t.pdf).

> [!TIP]
> The wording here is a little confusing: if the employer now has the employee's W-4, what makes this calculation tentative?
> It's called the "tentative withholding" because it doesn't yet account for the taxpayer's tax credits (Line 3a on Pub15-T, Worksheet 1A) and additional withholdings (Line 4a, Ibid.);
> the calculation that does incorporate those values is the "final amount to withhold" (Line 4b, Ibid.).

Finally, $\text{bonusWithholdings}(j)$ is what should be withheld from any bonuses the employee expects to earn at that job.
Mercifully, this calculation is very simple ([Pub 15](https://www.irs.gov/pub/irs-pdf/p15.pdf), Section 7: Supplemental Wages).

### Pay Period Calculations

Of the four terms that comprise the $\text{committedWithholding}$ calculation, past and bonus withholding are straightforward, because they do not require pay period calculations.
Let's dive into the ones that do.

Tax withholding calculations are based on pay periods.
The number of pay periods in a year based on payroll type is defined in Table 3 of Pub15-T, Worksheet 1A.

| Semianually | Quarterly | Monthly | Semimonthly | Biweekly | Weekly | Daily |
| --- | --- | --- | --- | --- | --- | --- |
| 2 | 4 | 12 | 24 | 26 | 52 | 260 |

Knowing how many pay periods are left in a job is crucial for withholding calculation, because the $\text{totalOwed}$ is paid over time, on each paycheck.
For example, if there are two months left in the year, and we project that $1000 of taxes need to be withheld from those earnings, then $500 needs to be withheld from each remaining paycheck if the taxpayer is paid monthly, but $250 needs to be withheld from each paycheck if they are paid semimonthly.

Pay periods therefore factor into the calculations for both $\text{lagWithholding(j)}$ and $\text{futureTentativeWithholding(j)}$.

```math
\text{lagWithholding}(j) = \text{currentWithholding}(j) * \text{lagPayPeriods}(j)
```

$\text{currentWithholding}(j)$ is a value the taxpayer provides—it's what is currently being withheld from their paycheck.
This only applies if they are filling out new W-4s mid-year; if this is for a new job, the amount will be zero.
The $\text{lagPayPeriods}(j)$ are, as previously discussed, based on the assumption that it will take about two weeks to process the W-4.
(i.e. $\text{lagPayPeriods}(j)$ will be 2 if the taxpayer is paid weekly, 1 if they are paid biweekly.)

```math
\text{futureTentativeWithholding}(j) = \text{tentativeWithholdingAmount}(j) * \text{remainingPayPeriodsAfterLag}(j)
```

The $\text{futureTentativeWithholding}(j)$ calculation is conceptually similar, but it has different terms.
$\text{tentativeWithholdingAmount}(j)$ is essentially Pub15-T, Step 2h, Worksheet 1A.
It applies the standard withholding schedules to the employee's income, and then divides them by the number of remaining pay periods (after lag).
TWE multiplies that by the number of remaining pay periods (after lag) to get the standard amount that will be withheld by this job through the end of the year.

Calculating the number of remaining pay periods in a year is deceptively complicated, partially based on assumptions made by TWE.
For instance, if you work at the end of December but get paid for that work in January, that income gets applied to the following tax year, not this one.
TWE also accounts for the possibility that the year will end before the lag periods are up.
For example, if there is one week left in the year and the taxpayer is paid weekly, $\text{lagPayPeriods(j)}$ will be one instead of two, and $\text{remainingPayPeriodsAfterLag(j)}$ will be zero.

### Partial Pay Periods

There is an important contingency that the Pub15-T does not explicitly handle: what happens if the taxpayer leaves a job in the middle of the pay period?

In this case, TWE will calculate a fractional withholding amount and add it to the total withholdings for that job.

```math
\text{fractionalWithholding}(j) = \text{w} * (\text{a}/\text{b})
```

In this formula, $a$ is the number of days worked in the fractional pay period, $b$ is the number of days in a full pay period, and $w$ is *either* $\text{currentWithholding}$, if the partial pay period occurs during the lag period, or $\text{futureTentativeWithholding}$ otherwise.

## Filling out the W-4

Once TWE has calculated the tax gap, it automatically filling out a W-4.
There are only 4 lines that need to be produced.

- Line 3: Credits & Payments
  - Case 1: Needs to withholding more = positive tax gap = 0:
  - Case 2: Job is not the highest paying job among the various jobs the taxpayer is filling out the W-4  = 0:
  - Case 3: Negative tax gap OR highest paying job = MIN(Pub 15-T tentative withholding amount per pay period * pay periods per year, projected and annualized tax excess/pay periods once W-4 change occurs)

- Line 4(a): Other income
  - Case 1: other income - adjustments < 0 = negative net change = 0
  - Case 2: Job is not the highest paying job among the various jobs the taxpayer is filling out the W-4  = 0
  - Case 3: other income - adjustments = positive net change

- Line 4(b): Deductions
  - Case 1: other income - adjustments > 0 = positive net change = 0
  - Case 2: other income - adjustments < 0 AND job will go to year end = proportion of the job's income through year end, either relative to other jobs or just through standard withholding

- Line 4(c): Desired extra withholdings per pay period
  - Case 1: negative tax gap = refund, don't need to withhold more = 0
  - Case 2: job is not the highest paying job = 0
  - Case 3: positive tax gap = tax gap / pay periods after the W-4 change takes effect

n.b. If Line 4(a) is positive, 4(b) should be zero and vice versa.
