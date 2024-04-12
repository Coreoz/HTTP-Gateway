HTTP Gateway Publishers
=======================
This module provides `org.reactivestreams.Publisher` utilities to manipulate upstream and downstream byte flows.

BytesReaderPublishers
---------------------
TODO
- [ ] Create StaticBytesPublisher to provide the ability to create another Publisher from captured bytes
- [ ] explain how to use this to capture request/response bodies (with memory & security considerations though: if not verified, someone could just upload Go of data until an OOM appears)
- [ ] Reference and use OpenApiHttpFetcherTest for usage. Reference that in the core module docs to explain how to mock gateway response. Maybe also add some code/docs in the test module.
- [ ] use this in a sample project
