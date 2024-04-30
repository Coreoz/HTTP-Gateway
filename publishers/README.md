HTTP Gateway Publishers
=======================
This module provides `org.reactivestreams.Publisher` utilities to manipulate upstream and downstream byte flows.

BytesReaderPublishers and StaticBytesPublishers
-----------------------------------------------
These two publishers enable to:
- `BytesReaderPublishers`: Read the content of a `Publisher` as a byte array
- `StaticBytesPublishers`: Create a new `Publisher` from a byte array

So these 2 classes could be used to read a request/response body and then to stream it back to follow the normal execution flow.

Memory considerations must be taken into account when using these classes because loading big amount of data in memory may lead to `OutOfMemory` exceptions.
The `maxBytesToRead` parameter of `BytesReaderPublishers` must be chosen wisely depending on the amount of JVM memory available, the request/response body expected size (and anticipate an attack of someone who would send a big payload), the requests concurrency.

See `BytesReaderPublishersTest` for some exemple usage.

PublisherPeeker
---------------
This `Publisher` enables to view the data that are going though a `Publisher`.
This is especially useful for logging purposes. 

TODO
----
- [ ] Create a PublisherMerger class to merge 2 publisher: when one is terminated, the other is used. This could be used when a small part of a request body is read and we want to send what has already been read, and the base request body publisher
- [ ] use this in a sample project
