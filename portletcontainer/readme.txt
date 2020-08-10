Place the two jsp files in this directory in the following directory:

<SunAppServerRoot>\domains\domain1\applications\j2ee-modules\portletdriver

This will force all portlets with the word "DEVELOPMENT" in their titles (without the quotes) to NOT be displayed unless the user has logged in. Also, deployment/undeployment of portlets is now restricted to logged in users.
