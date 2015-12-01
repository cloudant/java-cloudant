# Unreleased
- [IMPROVED] Included error and reason information in message from `CouchDbException` classes.
- [IMPROVED] Added HTTP status code to `Response` objects.
- [FIX] Too many bytes written exception caused by inconsistent encoding between UTF-8 and the
  JVM default. UTF-8 is now correctly used for the request body content length and throughout.
- [FIX] Fixed deserialization of `ReplicatorDocument` where the source or target url is a JSON
  object not a string.
- [FIX] Renew cookies when the server returns a 403 status code with `{"error":"credentials_expired"}`.
- [FIX] Cookie authentication now honours custom SSL configurations when making the `_session`
  request.

# 2.0.0 (2015-11-12)
- [NEW] `DesignDocument.MapReduce` now has a setter for the `dbcopy` field.
- [NEW] Requests for the `_all_docs` endpoint are made via `Database#getAllDocsRequestBuilder()`
  instead of using a view.
- [NEW] Introduced new view query API. More information is available in the javadoc,
  including usage and migration examples. Note the absence of an equivalent for `queryForStream()`.
  If you were using the `queryForStream()` method we would be interested in feedback about your use case.
  For example, if you were using the `InputStream` directly for streaming API parsing with an alternative
  JSON library we might be able to make this easier by handling the streams and providing a callback.
- [NEW] Optional OkHttp dependency for per CloudantClient instance connection pooling.
- [BREAKING CHANGE] Removed `Database.batch(Object)` method. Using `batch=ok` is not recommended.
- [BREAKING CHANGE] JVM `http.maxConnections` configured pool is used by default for connection pooling.
- [BREAKING CHANGE] Removed Apache HttpClient dependency. API methods that used HttpClient classes
  (e.g. `executeRequest`) now use `HttpConnection` instead.
- [BREAKING CHANGE] `CloudantClient` public constructors and `ConnectionOptions` have been removed.
  `CloudantClient` instances are now created and have options configured using `ClientBuilder`.
- [BREAKING CHANGE] Removed these deprecated methods:
    * `CloudantClient.deleteDB(String, String)` use `CloudantClient.deleteDb(String)`,
    * `Database.invokeUpdateHandler(String, String, String)` use `Database.invokeUpdateHandler(String, String, Params)`,
    * `CloudantClient.setGsonBuilder(GsonBuilder)` use `ClientBuilder.gsonBuilder(GsonBuilder)`.
- [BREAKING CHANGE] Removed version 1.x view query API.
- [BREAKING CHANGE] LightCouch classes moved to package com.cloudant.client.org.lightcouch.
  This should only have a visible impact for `CouchDbException` and its subclasses.
- [BREAKING CHANGE] Removed DbDesign class and replaced with DesignDocumentManager.
  If you were using the `getFromDesk` method, convert your design document directory to javascript
  files and use `DesignDocumentManager.fromFile(File)` or `DesignDocumentManager.fromDirectory(File)`.
  More information is available in the javadoc, including usage for de-serializing design document
  javascript files to `DesignDocument` objects.
- [FIX] Use the default port for the protocol when a client instance is created from a URL without
  specifying a port.

# 1.2.3 (2015-10-14)
- [NEW] Added Basic Auth for HTTP proxies. Configure via `ConnectOption#setProxyUser`
  and `ConnectOptions#setProxyPassword`.

# 1.2.2 (2015-10-01)
- [CHANGED] Added default of 5 minutes for both connection and socket timeouts instead of waiting forever.
- [IMPROVED] Upgraded Apache HttpClient from 4.3.3 to 4.3.6.

# 1.2.1 (2015-09-14)
- [FIXED] `org.apache.http.conn.UnsupportedSchemeException: http protocol is not supported`
  when using a proxy server with `http` and a server with `https`.

# 1.2.0 (2015-09-10)
- [FIXED] `NullPointerException` when parsing `{doc: null}` JSON in search or view results.
- [FIXED] Fixed issue with pagination numbering when using `queryPage` with
  a clustered DB.
- [FIXED] Fixed issue where `queryPage` could not handle JSON values emitted from views.  
- [IMPROVED] Various documentation updates.
- [DEPRECATED] `com.cloudant.client.api.model.Page` setter methods.

# 1.1.2 (2015-08-14)

- [IMPROVED] Removed unconditional GET request when creating `Database` objects.
  This offered little protection to the developer while performing a hidden HTTP request.
- [FIXED] Fixed issue where URL parameters would be appended rather than replaced. This could cause very
  long URLs to be accidentally created.

# 1.1.0 (2015-07-31)

- [BREAKING CHANGE] Hostname verification and certificate chain
  validation are now enabled by default. To disable these additional
  checks when the client connects to the database,
  `setSSLAuthenticationDisabled(true)` can be called on the `ConnectOptions`
  object before you pass it to the `CloudantClient` constructor.
- [NEW] New API for specifying the SSLSocketFactory. This can used to
  enhance security in specific environments. To set this SSLSocketFactory,
  call the `setAuthenticatedModeSSLSocketFactory` method on the
  `ConnectOptions` object before you pass it to the `CloudantClient`
  constructor.
- [NEW] New API for deleting databases, `CloudantClient.deleteDB(String name)`
- [FIX] Fixed querying of next/previous page in a descending view.
- [FIX] Fixed handling of non-ASCII characters when the platform's
  default charset is not UTF-8.
- [FIX] Fixed encoding of `+`, `=` and `&` characters when they are used
  in the query part of a URL.
- [IMPROVED] Changed the default number of connections per host to 6.
- [NEW] use_index option now available for `FindByIndex` .
- [IMPROVED] Use Cloudant API V2 for permissions.
- [NEW] Added user-agent header to requests.
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
