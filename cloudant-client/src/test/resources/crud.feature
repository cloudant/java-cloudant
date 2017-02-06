Feature: CRUD

  Scenario: read document
    Given a document named Foo with fields hello equals world exists
    When I read a document named Foo
    Then the document has the same id and fields
