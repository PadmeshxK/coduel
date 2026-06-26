package com.coduel.interfaces;

import com.coduel.common.exception.ApiException;

/**
 * Port for STORING media bytes only — no validation, no processing (that's business logic in the flow).
 * Returns a public URL the client can render. Dev = local disk; prod = swap in an S3/R2 adapter behind
 * this same contract (the URL just becomes an absolute CDN URL).
 */
public interface MediaVault {

    /**
     * Persist already-processed bytes under a fresh name with the given extension, inside the given
     * folder (a key prefix, e.g. "chat" or "voice"); return its public URL.
     */
    String store(byte[] data, String extension, String folder) throws ApiException;
}
