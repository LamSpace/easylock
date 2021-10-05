package io.github.lamtong.easylock.client.lock;

import io.github.lamtong.easylock.common.core.Request;

/**
 * Constant definitions of {@link Request} errors.
 *
 * @author Lam Tong
 * @version 1.3.2
 * @see Request
 * @since 1.2.0
 */
final class RequestError {

    static final String EMPTY_LOCK_KEY = "Lock key should not be null or empty, reset lock key.";

    static final String LOCKING_ALREADY = "Locking succeeds already, lock cancels.";

    static final String LOCKING_FAIL = "Locking fails before, unlock cancels.";

    static final String UNLOCKING_ALREADY = "Unlocking succeeds already, unlock cancels.";

    private RequestError() {
    }

}