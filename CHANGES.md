# 2.13.1 (2018-09-05)
- [FIXED] Regression that prevented `keys` being included as part of a `ViewMultipleRequest`.

# 2.13.0 (2018-06-06)
- [NEW] Add `CloudantClient.metaInformation()` to get meta information from the welcome page.
- [NEW] Add methods for interacting with the replicator `_scheduler` endpoint:
  - `CloudantClient.schedulerJobs()`,
  - `CloudantClient.schedulerDocs()`,
  - `CloudantClient.schedulerDoc(docId)`.
- [NEW] Add `ComplexKey.addHighSentinel()` to allow matching of all values as part of a complex key
  range.
- [NEW] Support IAM authentication in replication documents.
- [NEW] Add support for `stable` and `update` parameters in views.
- [FIXED] Issues retrieving deleted documents using an `AllDocsRequest`.
- [FIXED] An issue where `getReason()` returned an incorrect value for `Response` objects returned
  by `Database.bulk()`.
- [FIXED] Added missing `_deleted` field to `Document` model class.
- [BREAKING CHANGE] The fix which adds `_deleted` to `Document` model class will break
  (de)serialisation for classes which sub-class `Document` and themselves declare a field which
  implicitly or explicitly has the serialised name of `_deleted`. The suggested work-around is to
  remove this field from classes which sub-class `Document`. For more details, see the javadoc for
  `Document`.
- [DEPRECATED] OAuth authentication API `targetOauth` on the `Replication` class.
- [DEPRECATED] `stale` parameter in views.
- [IMPROVED] Added support for IAM API key in the client builder `bluemix` method.
- [IMPROVED] When making view requests (including `_all_docs`), set `keys` in the `POST` body rather
  than in `GET` query parameters. This is because a large number of keys could previously exceed the
  maximum URL length, causing errors.

# 2.12.0 (2018-02-08)
- [NEW] Index creation APIs and builders including support for text and partial indexes.
- [NEW] Support for query bookmarks and execution stats.
- [NEW] Utilities for generating selectors for queries and partial indexes.
- [IMPROVED] Throw an `IllegalArgumentException` with a better message if trying to build the client
  with a `null` URL instead of a `NullPointerException`.
- [FIXED] Updated default IBM Cloud Identity and Access Management token URL.
- [FIXED] An issue where a row was truncated from an `_all_docs` response including deleted docs for
  a request using `keys` where the server returns a `total_rows` count only for undeleted docs.
- [DEPRECATED] Old index creation and listing APIs:
    - `com.cloudant.client.api.model.Index`
    - `com.cloudant.client.api.model.IndexField`
    - `com.cloudant.client.api.Database.createIndex(java.lang.String, java.lang.String, java.lang.String, com.cloudant.client.api.model.IndexField[])`
    - `com.cloudant.client.api.Database.listIndices`

# 2.11.0 (2017-11-21)
- [NEW] Added an extra bluemix method to the client builder allowing a custom service name to be
  used with the VCAP_SERVICES environment variable content.
- [FIXED] An issue where `Changes.hasNext()` never returns on receipt of `last_seq` for continuous
  changes feeds.

# 2.10.0 (2017-11-07)
- [NEW] Added API for upcoming IBM Cloud Identity and Access Management support for Cloudant on IBM
  Cloud. Note: IAM API key support is not yet enabled in the service.
- [IMPROVED] Updated documentation by replacing deprecated Cloudant links with the latest bluemix.net
  links.
- [IMPROVED] Clarified documentation for search indexes.
- [IMPROVED] Added `Row#getError` and `AllDocsResponse#getErrors` for returning any error messages
  from a `view` or `_all_docs` request.
- [FIXED] Connection leaks in some session renewal error scenarios.
- [FIXED] IllegalStateException now correctly thrown for additional case of calling
  `MultipleRequestBuilder#build()` before `add()` was called.
- [UPGRADED] Optional OkHttp dependency to version 3.8.1.
- [DEPRECATED] The `dbCopy` setter and getter on the `MapReduce` class.

# 2.9.0 (2017-04-26)
- [NEW] Add faceted search variable argument to `drillDown` method allowing multiple drill down
  values to be specified for a single field name.
- [DEPRECATED] The `drillDown(String, String)` method. Use new `drillDown(String, String...)` which
  allows multiple drill down values.

# 2.8.0 (2017-02-15)
- [NEW] Added `bluemix` method to the client builder allowing service credentials to be passed using
  the CloudFoundry VCAP_SERVICES environment variable.
- [NEW] Add additional method to `GET` standalone attachments.
- [IMPROVED] Faster shutdown when using the optional OkHttp client.
- [IMPROVED] Add URL to `CouchDbException` exception message (where applicable) for easier debugging.
- [FIXED] Issue with "+" (plus) not being regarded as a reserved character in URI path components.
- [FIXED] Issue with double encoding of restricted URL characters in credentials when using
   `ClientBuilder.url()`.

# 2.7.0 (2016-11-21)
- [UPGRADED] Optional okhttp dependency to version 3.4.2.
- [FIXED] `NullPointerException` when calling `AllDocsResponse.getIdsAndRevs` for a request with
  multiple non-existent keys (IDs).
- [IMPROVED] Preserved path elements from `URL`s used to construct a `ClientBuilder`.
  This allows, for example, a `CloudantClient`connection to use a gateway with a
  `URL` like `https://testproxy.example.net:443/cloudant/mydb`.
- [FIXED] Issue where DesignDocumentManager did not close a `FileInputStream`.

# 2.6.2 (2016-09-20)
- [FIX] `NoClassDefFoundError: com.squareup.okhttp.Authenticator` for version 2.6.1 if the optional
  okhttp dependency was not included.

# 2.6.1 (2016-09-15)
- [FIX] `NoClassDefFoundError: com.squareup.okhttp.Authenticator` for version 2.6.0 if the optional
  okhttp dependency was not included.

# 2.6.0 (2016-09-12)
- [NEW] Enabled `reduce` and other reduce related parameters to be set when using
  `MultipleRequestBuilder`.
- [FIX] Consumed response streams in `client.shutdown()` and `CookieInterceptor` to prevent
  connection leaks.
- [NEW] Added functionality to remove attachment from document by attachment name.
- [FIX] Issue authenticating with a proxy server when connecting to a HTTPS database server.
- [FIX] Throw an `IllegalArgumentException` if using an unsupported proxy type.
- [IMPROVED] Documentation for connecting to and authenticating with proxy servers.
- [FIX] `java.lang.StringIndexOutOfBoundsException` when trying to parse `Set-Cookie` headers.
- [FIX] `NullPointerException` in `CookieInterceptor` when no body was present on response.
- [UPGRADED] Upgraded GSON to 2.7
- [IMPROVED] Added warning messages for JVM DNS cache configuration settings that could impede
  client operation during cluster failover.

# 2.5.1 (2016-07-19)
- [IMPROVED] Made the 429 response code backoff optional and configurable. To enable the backoff add
  an instance of a `Replay429Interceptor` with the desired number of retries and initial backoff:
  `ClientBuilder.account("example").interceptors(new Replay429Interceptor(retries, initialBackoff))`
  A default instance is available using 3 retries and starting with a 250 ms backoff:
  `Replay429Interceptor.WITH_DEFAULTS`. To replicate the backoff of version 2.5.0 create an instance
   using `new Replay429Interceptor(10, 250l)`.
- [FIX] Fixed places where streams where not closed and could cause connections to leak.

# 2.5.0 (2016-05-24)
- [NEW] Handle HTTP status code `429 Too Many Requests` with blocking backoff and retries.
- [NEW] Added `DesignDocumentManager.list()` to return all design documents defined in a database.
- [NEW] Added an optional `SettableViewParameters.STALE_NO` constant for the default omitted case of
  the stale parameter on a view request.
- [NEW] Added `descending` option for changes feed.
- [NEW] Added `parameter` option for changes feed to allow specifying a custom query parameter on
  the request for example to be used by a filter function.
- [NEW] Added HttpConnection logging filters for HTTP request method and URL regex.
- [IMPROVED] Added additional logging output and documentation.
- [IMPROVED] Documentation for `Replication` class.
- [FIX] `JsonSyntaxException` when deserializing Cloudant query language generated design
  documents into the `DesignDocument` class.
- [FIX] `PreconditionFailedException` was never thrown when calling `createDB("dbname")` when the
  database already existed.
- [FIX] Documentation that suggested calling `database("dbname", false)` would immediately throw a
  `NoDocumentException` if the database did not exist. The exception is not thrown until the first
  operation on the `Database` instance.
- [FIX] `ClassCastException` when the server responded `403` with a `null` reason in the JSON.

# 2.4.3 (2016-05-05)
- [IMPROVED] Reduced the length of the User-Agent header string.
- [IMPROVED] Use a more efficient HEAD request for getting revision information when using
  `DesignDocumentManager.remove(String id)`.
- [FIX] Regression where `_design/` was not optional in ID when using `DesignDocumentManager` methods.
- [FIX] Issue where `use_index` was specified as an array instead of a string when only a design
  document name was provided.
- [FIX] Issue where empty array was passed for `use_index` option when `FindByIndexOptions.useIndex()`
  was not used.
- [FIX] Incorrect method names in overview documentation example for connecting to Cloudant service.

# 2.4.2 (2016-04-07)
- [IMPROVED] Use the JVM default chunk size for HTTP content of unknown length.
- [FIX] Regression where `JsonParseException` would be thrown if `Database.findByIndex` selector
  contained leading whitespace.

# 2.4.1 (2016-03-10)
- [NEW] Documentation for logging in project javadoc `overview.html`.
- [UPGRADED] Upgraded optional okhttp to 2.7.5.
- [FIX] Issues with the changes feed, replication, or getting database info when using Cloudant Data
  Layer Local Edition because sequence IDs were incorrectly always treated as strings not JSON.
- [FIX] Issue of `javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure` on
  IBM Java with okhttp. SSL connections using okhttp are now configured to use the JVM enabled
   protocols and cipher suites in the same way as the `HttpURLConnection`.
- [FIX] Issue where a `java.net.ProtocolException` was thrown if the cookie had expired when a
  request that included a body was sent. Note that the client no longer uses the
  `Expect:100-continue` header on requests.
- [FIX] Fix issue where design documents would not be updated if only the
  `indexes` field was updated.
- [FIX] The `equals` and `hashcode` methods for `Document` and `ReplicatorDocument` failed to compare
  revision identifier and some other fields.
- [FIX] Issue where connections could leak because streams were not closed.
- [DEPRECATED] The `InputStream` setters `HttpConnection.setRequestBody(InputStream)` and
  `HttpConnection.setRequestBody(InputStream, long)`. Use of the new `InputStreamGenerator` is
  preferred because it allows for request replays.
- [CHANGE] Moved HTTP and interceptor code into a separate jar.

# 2.3.0 (2016-02-12)
- [NEW] Constructor for `Database` subclasses.
- [IMPROVED] Documentation for `Database.findByIndex` to show complete selector.
- [IMPROVED] Documentation regarding document revisions.
- [FIX] `CouchDbException: 400 Bad Request: bad_request: invalid_json` when a
  query parameter contains a semicolon.
- [FIX] `NullPointerException` when using `Database.saveAttachment` with a `null`
  revision to attach to a new document with the specified ID.
- [FIX] `CouchDbException` when using `Database.saveAttachment` to update
  attachments.

# 2.2.0 (2016-01-08)
- [IMPROVED] Request a session delete on client shutdown.
- [IMPROVED] Consistently encode all parts of request URLs and handle additional special characters.
- [FIX] Stopped integers in complex key arrays turning into floats when using view pagination with tokens.
- [FIX] Replaced string operations with GSON objects when parsing JSON.
- [FIX] Enabled specification of multiple drilldown parameters for search.
- [NEW] `Database.invokeUpdateHandler` now handles POST requests.

# 2.1.0 (2015-12-04)
- [IMPROVED] Included error and reason information in message from `CouchDbException` classes.
- [IMPROVED] Added HTTP status code to `Response` objects.
- [IMPROVED] Added parameter pagination option for views. See `ViewRequest.getResponse(String)`.
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
- [UPGRADED] Upgraded Apache HttpClient from 4.3.3 to 4.3.6.

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
