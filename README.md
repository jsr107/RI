JCache Reference Implementation
------------------------

This is the reference implementation for JCache.

This implementation is not meant for production use. For that we would refer you to one of the many open source and commercial
implementations of JCache.

The RI is there to ensure that the specification and API works.

For example, some things that we leave out:

- tiered storage. A simple on-heap store is used.
- replicated or distributed caching

Why did we do this? Because a much greater engineering effort, which gets put into the open source and commercial caches
which implement this API, is required to accomplish these things.

Having said that, the RI uses the Apache 2.0 license and is a correct implementation of the spec. It can be used to create new cache
implementations.
