#!/bin/bash
#
#Script to create the MySQL database and user for PointArchiverMySQL.
#

echo "CREATE database MoniCA;" | mysql --user=root mysql
echo "GRANT ALL PRIVILEGES ON MoniCA.* TO 'monica'@'localhost';" | mysql --user=root mysql
