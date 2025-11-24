# Tax Withholding Basics

> [!WARNING]
> These are not official tax filing instructions.
> This is a basic summary provided to help a developer understand the structure and layout of the Tax Withholding Estimator.
> The information included here is not guaranteed to be up-to-date or correct.
> See the Legal Notice in the README for more.

Under most circumstances, if you earn money in the United States, you are obligated to pay taxes on those earnings throughout the year.
You can't just earn all the money and then pay your taxes during tax season;
doing so might result in an underpayment penalty.

Per [Pub505](https://www.irs.gov/publications/p505), there are two ways to pay as you go:

1. Withholding - if you are an employee, your employer probably withholds your estimated income tax burden from your pay.
If you have earnings that your employer is unaware of, you can instruct them to withhold additional amounts, which are paid to the IRS in your name.
2. Estimated tax - you can make quarterly estimated payments on your income directly to the IRS.
This is typically how you pay taxes if you are self-employed or derive significant income via other means such as capital gains, rents, and dividends.

When you start a job, you submit a W-4 to the employer that given them the information they need to withhold the appropriate amount from your paycheck.

The W-4 is a [relatively simple, four-page form](https://www.irs.gov/pub/irs-pdf/fw4.pdf), consisting of one page that the taxpayer submits to the employer, and three pages of information and supplemental worksheets.
Broadly speaking, the paper W-4 is capable of estimating the taxpayer's withholdings if they (or they and their spouse) have three or fewer jobs, have no other sources of income, and do not change jobs during the year.
Many taxpayers fall within this category, but many do not.
For the ones that do not, the paper W-4 repeatedly directs them to use the Tax Withholding Estimator (TWE) found at [www.irs.gov/W4App](www.irs.gov/W4App).

It is the Tax Withholding Estimator's job to provide W-4s for taxpayers whose tax scenarios fall outside the scope of the attached worksheets.
Taxpayers fill out the forms on TWE, and it generates a PDF W-4 with the correct amount of additional withholdings.

## Calculating The Tax Gap

In essence, TWE needs to figure out the tax gap: the difference between what the taxpayer will owe at the end of the year and what they have already committed to withholding from their paycheck(s).

```math
\text{taxGap} = \text{totalOwed} - \text{totalCommittedWithholding}
```

Ideally, by adjusting their withholdings, the taxpayer will end the year with a tax gap of zero: owing nothing and being owed nothing by the IRS.
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
\text{AGI} = \text{totalIncome} - \text{adjustments}
```

Deductions are then subtracted from the AGI to arrive at taxable income.
AGI is also used to determine eligibility for various credits.


```math
\text{taxableIncome} = \text{AGI} - \text{deductions}
```

That $\text{taxableIncome}$ value is then used to calculate your $\text{tentativeTaxBurden}$, via the [tax table](https://www.irs.gov/instructions/i1040tt#idm139749562274528) indicated by the [Line 16 instructions](https://www.irs.gov/pub/irs-pdf/i1040gi.pdf).
Various additional taxes (like self-employment tax, if necessary) and credits can be applied as well, to arrive at the $\text{totalOwed}$ value.

Altogether, this is essentially what the 1040 calculates: the total amount of money that you owe to, or are owed by, the US Government.

### Total Committed Withholding

The second term required to calculate the $\text{taxGap}$ is $\text{totalCommittedWithholding}$.
This is where the bulk of tax calculation complexity lies.

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

It's called the "committed" withholding because it's money that has either already been paid to the government, or will be paid before the W-4 can take effect.
For a single job $j$, the committed withholding is calculated as follows:

```math
\text{committedWithholding}(j) =
  \text{pastWithholding}(j) +
  \text{lagWithholding}(j) +
  \text{futureTentativeWithholding}(j) +
  \text{bonusWithholding}(j)
```

Let's break down each of these terms one-by-one.

$\text{pastWithholding}(j)$ is simply the amount that has already been withheld from the taxpayer's paychecks for this job, this year, so far.

$\text{lagWithholding}(j)$ is what TWE expects the employer will withhold after the W-4 is submitted, but before the W-4 is processed and its changes are reflected on the taxpayer's paycheck.
This is a **major assumption**.
Effectively, TWE guesses that it will take about 2 weeks from the time the taxpayer fills out the estimator to when the employer will update their payroll with the new values derived from the W-4.

$\text{futureTentativeWithholding}(j)$ is what the employer will calculate they should withhold from the taxpayer's paycheck, based on the values the taxpayer fills out the W-4 with, *not including any additional withholdings*.
Think of this as the "standard amount" that should be withheld from a paycheck, given information provided by the taxpayer about things such as their filing status and number of dependents.
This is step 2h on Worksheet 1A of the [Pub15-T](https://www.irs.gov/pub/irs-pdf/p15t.pdf); more on that in a moment.

> [!TIP]
> The wording here is a little confusing: if the employer now has the employee's W-4, what makes this calculation tentative?
> It's called the "tentative withholding" because it doesn't yet account for the taxpayer's tax credits (Line 3a on Pub15-T, Worksheet 1A) and additional withholdings (Line 4a, Ibid.);
> the calculation that does incorporate those values is the "final amount to withhold" (Line 4b, Ibid.).

Finally, $\text{bonusWithholdings}(j)$ is what should be withheld from any bonuses the employee will earn at that job.
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

$\text{currentWithholding}(j)$ is a number the taxpayer gives us—it's what is currently being withheld from their paycheck.
This only applies if they are filling out new W-4s mid-year; if this is for a new job, the amount will be zero.
The $\text{lagPayPeriods}(j)$ are, as previously discussed, based on the assumption that it will take about two weeks to process the W-4.
(i.e. $\text{lagPayPeriods}(j)$ will be 2 if the taxpayer is paid weekly, 1 if they are paid biweekly.)

```math
\text{futureTentativeWithholding}(j) = \text{tentativeWithholdingAmount}(j) * \text{remainingPayPeriodsAfterLag}(j)
```

The $\text{futureTentativeWithholding}(j)$ calculation is conceptually similar, but it has different terms.
$\text{tentativeWithholdingAmount}(j)$ is essentially Pub15-T, Step 2h, Worksheet 1A.
It applies the standard withholding schedules to the employee's income, and then divides them by the number of remaining pay periods (after lag).
TWE immediately multiplies that by the number of remaining pay periods (after lag) to get the standard amount that will be withheld by this job through the end of the year.

> [!TIP]
> TWE also accounts for the possibility that the year will end before the lag periods are up.
> For example, if there is one week left in the year and the taxpayer is paid weekly, $\text{lagPayPeriods(j)}$ will be one instead of two, and $\text{remainingPayPeriodsAfterLag(j)}$ will be zero.

### Partial Pay Period Calculations

There is an important contingency that the Pub15-T does not explicitly handle: what happens if the taxpayer leaves a job in the middle of the pay period?
