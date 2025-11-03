# IRS Onboarding

## Configure commit email address

All commits from IRS staff on this project must be authored using your GitHub-provided `noreply` email address.

To achieve this, you'll need to make sure that commits authored via the GitHub web UI as well as local `git` use the GitHub-provided `noreply` email address:

1. Enable private email address
    1. Go to [your GitHub email settings](https://github.com/settings/emails)
    2. Enable `Keep my email address private`
    3. Note and copy the GitHub email address that is displayed. It looks like `{ID}+{USERNAME}@users.noreply.github.com` ([reference](https://docs.github.com/en/account-and-profile/reference/email-addresses-reference#your-noreply-email-address))
    4. Enable `Block command line pushes that expose my email` (Optional)
2. Set your local `git` email address for authoring commits
    1. Configure your local `git` `user.email` setting
        * If you would like to use your no-reply email address for _all_ local development, run
           ```shell
           git config --global user.email "YOUR_NO_REPLY_EMAIL"
           ```
        * If you prefer to only use the no-reply email address for development in your Tax Withholding Estimator working copy, run
          ```shell
          # Ensure you're in the directory for this git project
          cd ./path/to/tax-withholding-estimator/
          # Set your git email config for this repository only
          git config user.email "YOUR_NO_REPLY_EMAIL"
          ```
    2. Verify the configuration by having `git` echo the `user.email` configuration
       ```shell
       $ git config --global user.email
       YOUR_ID+YOUR_USERNAME@users.noreply.github.com
       ```

For more details, see:
* [Setting your commit email address](https://docs.github.com/en/account-and-profile/how-tos/setting-up-and-managing-your-personal-account-on-github/managing-email-preferences/setting-your-commit-email-address#setting-your-email-address-for-a-single-repository).
* [Your no-reply email address](https://docs.github.com/en/account-and-profile/reference/email-addresses-reference#your-noreply-email-address)

