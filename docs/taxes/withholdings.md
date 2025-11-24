# RFC: Withholdings
Last updated: 11/7/2025
Authors: sps-irs

# Introduction
The goal of the Tax Withholding Estimator (TWE) is two-fold. 

First, to estimate your tax gap for the tax year and then proposing a strategy for updating your withholdings based on reducing this tax gap to zero (if you want to break even) or some number slightly positive number (if you want a refund). The strategy is implemented in Form W-4, which you submit each time you want your employer to automatically withhold income tax from your paycheck. For those taxpayers with multiple jobs, the strategy might include adjusting withholdings across all the jobs, which would result in multiple W-4s.

Second, to estimate your withholding gap for the tax year if you do not make any changes or submit a revised W-4. 

The below goes through both tax gap and withholding gap and breaks down the core derivation logic for each category. We will then bring it all together for how the W-4 is actually produced based on these two values.

# Why is this hard?

"But sps-irs", you might say, "why can't I just follow the W-4 instructions and Pub-15T Worksheets, they seems easy enough?" Well yes, they are easy to follow and calculate, **but** there are many assumptions included in these calculations that break when performing a more fine grained estimation. Specifically, many taxpayers update their W-4 when a sudden life event occurs, such as changing jobs, and these changes in jobs do not always occur within the neat boundaries of Pub-15T. For instance, there is often a lag between when a job ends, a new one starts, and the updated W-4 is implemented for the new job. Similarly, taxpayers might have a partial pay period when one job ends and another partial pay period when another begins, such that the number of pay periods per job will not be an integer as assumed by Worksheet 1a. These types of nuances are functional requirements for TWE and must be incorporated.

# Tax Gap
## Core components
Tax gap means the total tax taxpayer owes (/totalTax) in taxes minus the total taxes they anticipate paying by the end of the year, in the form of estimated tax payments (/totalEstimatedTaxesPaid) or withholdings (/totalCommittedWithholdings).

> /taxGap = /totalOwed -
/totalCommittedWithholding
> /taxGap = /totalTax - /totalEstimatedTaxesPaid -
/totalCommittedWithholding

Put differently, the tax engine of TWE calculates two core outputs: /totalOwed and /totalCommittedWithholding. /totalOwed is the more straightforward of the two, as it is just the total tax liability minus the total credits and total advance payments made during the tax year. This is effectively line 37 of the 1040.

> /totalOwed = /totalTax -/totalEstimatedTaxesPaid

and /totalTax maps to line 24 of the 1040
> /totalTax = /totalTaxMinusCredits + /selfEmploymentTax + /additionalMedicareTax + /netInvestmentIncomeTax...

That leaves us with /totalCommittedWithholding, which is significantly more complicated to estimate accurately. /totalCommittedWithholding is the projected withholding amount based on the updated rate once the taxpayer submits the W-4 and the W-4 changes take effect.

## Simplified Withholding Calculations
Each job derives its own amount of committed withholdings, which are then aggregated togther to calculated tax gap. This fact is /jobs/*/committedWithholding, and can be understood as follows:

> committed withholdings = YTD withholdings + future withholdings (whose withholding rates **will not** be based on W4 changes) + future withholdings (whose withholding rates **will** be based on W4 changes) + withholdings from bonuses

YTD withholdings and withholdings from bonuses are straightforward to calculate. YTD withholdings is a Writeable fact, and bonus withholding rates are calculated based on a $1,000,000 threshold.

Future withholdings are the hardest part to estimate, as they break down into two categories: 1) Withholding whose rates will change once the W4 changes take effect, and those which will not because of a lag in implementing the updated W-4 withholding amount (lag)
> future withholdings = future withholdings with updated W4 rate + future withholdings without updated W4 rate
> future withholdings without updated W4 rate = average withholding per pay period * MIN(full pay periods remaining in year, pay periods before W4 changes take effect)
> future withholdings with updated W4 rate = tentative withholdings * MAX(0,full pay periods remaining in year - pay periods before W4 changes take effect)

Let's break down this down further
1) Pay periods before W4 changes take effect: **This is an assumption** based on how long the payroll provider would update the withholding calculations, based on the pay frequency of the job. Weekly frequency = 2, biweekly or semi-monthly = 1, monthly = 0. This is also referred to as 'W-4 change lag' or simply 'lag' in the fact dictionaries
2) Tentative withholding: this is Worksheet 1A Line 2h, the amount of withholding based on the the Annual Method Percentage. It divdies the standard annual withholding amount by the number of pay periods per year based on pay frequency (52 for weekly, 26 for biweekly, 24 for semimonthly, 12 for monthly)

>/tentativeWithholdingAmount = standardAnnualWithholdingAmount/payPeriodsPerYear
>/standardAnnualWithholdingAmount = filing status specific mapping that compares the adjustedAnnualWageAmount to a base threshold with an excess percentage on top
> /adjustedAnnualWageAmount = max(0,annualized income - filing status specific amount)


# Withholding Gap
## Core components 
The withholding gap is the difference between the total TWE thinks you will owe at the end of the year (for simplified purposes of your annual tax return ) and the projected withholdings that would have already been paid to the IRS through payroll. 
> withholding gap = EOY total owed - EOY projected withholdings

## EOY Projected Withholdings
EOY projected withholding is conceptually intuitive, but computationally complex, for similar reasons as calculating committed withholdings. Conceptually, projected withholdings is just the total withholdings from all of your jobs and pensions, both year to date (YTD) and rest of year (ROY). withholding calculations for both pensions and jobs are similar, so we will just focus on the job use case with the understanding that pension logic mirrors it rather closely. 

EOY projected withholdings = actual YTD withholdings + projected rest of year withholdings

Actual YTD withholdings (jobs/*/yearToDateWithholding) is a Writable fact that the taxpayer provides in the income section. The project rest of year withholdings is where things become complicated (again)

## Rest of year withholding
Becuase this projection occurs at the job level, we need to account for the difference between past, current and future jobs. A past job is one that is complete, and thus there is no rest of year withholdings. Only current and future jobs can have ROY withholdings. 

ROY withholdings is the output of the number of pay periods remaining in the year (full and partial) times the average withholding of the job per pay period
> ROY withholdings = full pay periods * average withholding per pay period


#  Pay Periods
Now that we understand how committed withholdings and projected withholdings are calculated, we see that in both cases the most important variable is the number of pay periods left to be paid in the year (/jobs/*/fullPayPeriods)
> full pay periods = remaining pay periods - partial pay periods

## Remaining pay periods
There are many different permutations of how to calculate remainings pay periods (RPP) for a given job. This is because RPP is dependent on pay frequency and, importantly,each pay frequency has its own complexity and edge cases that need to be handled. The fact in question is /jobs/*/remainingPayPeriods. A full breakdown of how RPP is calculated is likely more efficient using a spreadsheet and/or looking at the fact dictionary.

<!-- TKTK TODO: break down the general mechanics of RPP, if feasible here -->

## Partial Pay Periods
Whether a pay period is a partial pay period (PPP) depends on if the actual end date of the job (/endDate) is before the effective end date of the job. The effect end date is the last day the taxpayer worked the job as far as income counting during the tax year is concerned. If it is, then there will be a partial pay period, otherwise there wouldn't. A full breakdown of how PPP is calculated is likely more efficient using a spreadsheet and/or looking at the fact dictionary.

<!-- TKTK TODO: break down the general mechanics of PPP, if feasible here -->

## Summary
At its core, the complexity of TWE is in how we calculate pay periods given the many different permutations that can occur across prior, current and future tax years vs. the various pay frequencies and ways they needed to be handled. Importantly, and as noted above, at least some of this complexity is due to assumptions about how taxpayers are paid and how payroll systems account for effective pay dates for tax purposes. Different assumptions would change the level of complexity needed in calculating pay periods, and thus withholdings.

# W-4
Once we have calculated the tax gap and withholding gap, we can proceed to filling out the W-4. On the surface, the W-4 is a simple, one page form, especially if you only had or will have one job this year. There are only 4 lines that need to be produced, based on the projections from TWE or the multiple job worksheet as needed based on the answer in Step 2(b).

Line 3: Credits & Payments
    Case 1: Needs to withholding more = positive tax gap = 0:
    Case 2: Job is not the highest paying job among the various jobs the taxpayer is filling out the W-4  = 0:
    Case 3: Negative tax gap OR highest paying job = MIN(Pub 15-T tentative withholding amount per pay period * pay periods per year, projected and annualized tax excess/pay periods once W-4 change occurs)


Line 4(a): Other income
    Case 1: other income - adjustments < 0 = negative net change = 0
    Case 2: Job is not the highest paying job among the various jobs the taxpayer is filling out the W-4  = 0
    Case 3: other income - adjustments = positive net change 

    n.b. If Line 4(a) is positive, 4(b) should be zero and vice versa.

Line 4(b): Deductions
    Case 1: other income - adjustments > 0 = positive net change = 0
    Case 2: other income - adjustments < 0 AND job will go to year end = proportion of the job's income through year end, either relative to other jobs or just through standard withholding

    n.b. If Line 4(a) is positive, 4(b) should be zero and vice versa.

Line 4(c): Desired extra withholdings per pay period
    Case 1: negative tax gap = refund, don't need to withhold more = 0
    Case 2: job is not the highest paying job = 0
    Case 3: positive tax gap = tax gap / pay periods after the W-4 change takes effect


