# mariadb4j-maven-plugin
Maven plugin that starts and stops a MariaDB instance for the integration test phase.

This is a Maven plugin wrapper around https://github.com/vorburger/MariaDB4j, a 
helpful tool for launching MariaDB from Java. I got it to a working state, but 
found that my use case was more complex than it could support, needing `mysqldump`
in addition to the other executables provided.
