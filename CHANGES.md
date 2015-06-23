# Unreleased

- [BREAKING CHANGE] Hostname verification and certificate chain
  validation are now enabled by default. To disable these additional
  checks when the client connects to the database,
  `setSSLAuthenticationDisabled(true)` can be called on the `ConnectOptions`
  object before you pass it to the `CloudantClient` constructor.
- [NEW] New API for deleting databases, `CloudantClient.deleteDB(String name)`
- [FIX] Fixed querying of next/previous page in a descending view.
- [FIX] Fixed handling of non-ASCII characters when the platform's
  default charset is not UTF-8.
- [FIX] Fixed encoding of `+`, `=` and `&` characters when they are used
  in the query part of a URL.
- [Deprecated] Deprecated `CloudantClient.deleteDB(String name, String confirm)`
  API.

# 1.0.1 (2015-02-04)

- [FIX] Fixed Issue #27: selector API takes JSON fragment only?
- [FIX] Fixed Issue #28: client constructor: clarify user vs api.
- [FIX] Fixed Issue #30: The maven jar is compiled to run on only
  Java 7 and up.
- [FIX] Fixed Issue #31: Timestamp JSON string to Java Timestamp
  Value - View.query(class).

# 1.0.0 (2015-01-16)

- First official release.
- [NEW] Includes support for connecting to Cloudant Local.

# 1.0.0-beta1 (2014-10-27)

- Initial release (beta).
