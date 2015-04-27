# Introduction #

Certain operations on the MoniCA server, such as setting control points or manipulating alarms, may, as a matter of policy, require the user to be authenticated. Authentication can be easily enabled by using a RADIUS server.

# Enabling RADIUS #

By default a MoniCA server has authentication turned off and any operation may be performed by any user. Turing the authentication backend on is just a matter of defining the appropriate fields in your config/monitor-config.txt configuration file:

```
##############################
# OPTIONS FOR AUTHENTICATION
# Comment the options out to disable authentication
# Name of the RADIUS server
RADIUSHost 127.0.0.1
# Port to contact the RADIUS server
RADIUSPort 1812
# Shared secret for the RADIUS server
RADIUSSecret testing123
```

# Privileged Operations #

If authentication is enabled, then a user must be properly authenticated in order to perform various operations.

  * Assigning values to points, through the Ice "setData" or the ASCII "set" network interface calls.
  * Acknowledging or shelving alarms.
  * Adding new points to the system.
  * Adding new SavedSetups to the system.

# Encryption #
## Ice Interface ##
If authentication is enabled then the username and password sent along with the relevant functions in the Ice interface must both be encrypted by using the RSA modulus and exponent available through the Ice getEncryptionInfo call.

## ASCII Interface ##
The username and password may be sent to the ASCII interface in plain text, but doing this is strongly discouraged.

A much better strategy is to obtain the RSA modulus and exponent by invoking the "rsa" or "rsapersist" calls in the [ASCII Interface](ClientASCII.md) and using this to encrypt both the username and the password prior to sending them to the server.

# RADIUS Authentication #
Once the server has received the username and password supplied by the client, it will attempt to verify the credentials by contacting the RADIUS server whose details were supplied in the configuration file.

The RADIUS AccessRequest packet sent to the server will include requesting client's IP address in the `NAS-IP-Address` attribute, which can be used by the RADIUS server as an additional field in authentication.

# freeradius #
The MoniCA server should work okay with any RADIUS server. One commonly used server is freeradius. Here is some simple configuration examples for freeradius:

### /etc/freeradius/huntgroups ###

This file can be used to group network address ranges into groups. For instance:

```
siteservers	NAS-IP-Address == 192.168.1.123
```

### /etc/freeradius/users ###

Lots of documentation is available, but here is an example which defines a local user called "myscript" who can only be authenticated if the request comes from a specific hunt group. All other users are authenticated against the operating system:

```
myscript Cleartext-Password := "testing", Huntgroup-Name == "siteservers"

DEFAULT  Auth-Type = System
```