# IdentityNow Services Events - Cloud Queue

As IdentityIQ has more flexible and configurable ways of handling events and responding to changes than IdentityNow, it may be useful for customers with a hybrid solution, to communicate events to IdentityIQ. IdentityIQ can perform checks, interact with end users (requesting permissions or more information) and take action on events and feed back and necessary information to IdentityNow.

Since IdentityIQ usually resides on-premise behind a firewall, protected from the Internet, it cannot directly receive ETS messages. Rather than that, IdentityIQ would call out itself to poll for events to handle.

The proposed solution here is a mechanism that receives messages from IdentityNow and queue them for IdentityIQ to pick up and process. The mechanism can also be used by other systems to get messages from the cloud. 

This part is the Cloud Queue where messages are stored and can be retrieved from.

| Property | Value |
| --- | --- |
| **Name** | ETS Cloud Queue |
| **Date** | 2023.01.11 |
| **Owner(s)** | Menno Pieters |
| **Developer(s)** | Menno Pieters |
| **Overview** |  |
| **Event Trigger(s)** | ALL |
| **Installation** | See below |
| **Configuration** | See below |
| **Troubleshooting** | |
| **Support** | Only tested against Java 8 and Tomcat 9 |
| **License** | LICENSE.txt |

# Build

Using maven:

```
mvn clean package
```

# Database deployment

Currently, only MySQL is supported.

Edit the DDL file `WEB-INF/database/mysql_create.sql`:
* Optionally update the database name (default `etscloudqueue`)
* Optionally change the service account user name (default `etscloudqueue`)
* For a remote (non-localhost) server, set the remove IP or use `'%'` instead of `'localhost'`
* Set a secure password (default `etscloudqueue`)

Using a MySQL client or command line, load the DDL file to create the necessary tables.

# Deployment

Deployment steps:
* Extract the `.war` file
* Edit the file `WEB-INF/classes/queue.properties`:
** Set the correct database URL, username and password
** Change the admin password (defaults to `admin`) and optionally change the admin username (default `spadmin`);
*** See below for the password hash generation.
* Update the password hash in the file and save it.
* ZIP up (or use the `jar` command) the `war` file again and deploy the `war` file in Tomcat 9.

To generate the password hash:
* First create a "salt", e.g 8 or more random alphanumeric characters: `WS4FKMxq`.
* Take the password, e.g. `admin`
* Concatenate the salt and password: `WS4FKMxqadmin`
* Create an SHA256 hash: 
```
echo -n "WS4FKMxqadmin" | openssl dgst -sha256 -binary | openssl enc -base64
```
* result: `RtRY5AdvEPnUCS16aj8txq82AtMYDfLrZnTKOcVqhvI=`
* Base64 encode the salt: 
```
echo -n "WS4FKMxq" | openssl enc -base64
```
* result: `V1M0RktNeHE=` 
* Concatenate `{SSHA256}` prefix, salt, `$` separator and the password hash:
* result: `{SSHA256}V1M0RktNeHE=$RtRY5AdvEPnUCS16aj8txq82AtMYDfLrZnTKOcVqhvI=`

Alternative way to generate the password in BeanShell:
```
String ssha256(String salt, String password) {
  if (Util.isNotNullOrEmpty(salt) && Util.isNotNullOrEmpty(password)) {
    String saltedPassword = salt + password;
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedHash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
      Encoder encoder = Base64.getEncoder();
      String hashString = encoder.encodeToString(encodedHash);
      String result = SSHA256PREFIX +
        encoder.encodeToString(salt.getBytes(StandardCharsets.UTF_8)) + 
        "$" + hashString;
      return result;
     } catch (NoSuchAlgorithmException e) {
      // Unsupported - return null;
     }
   }
  return null;
}```




