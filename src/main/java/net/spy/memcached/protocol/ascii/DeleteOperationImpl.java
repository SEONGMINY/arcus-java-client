// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.APIType;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.OperationType;
import net.spy.memcached.ops.StatusCode;

/**
 * Operation to delete an item from the cache.
 */
final class DeleteOperationImpl extends OperationImpl
        implements DeleteOperation {

  private static final int OVERHEAD = 32;

  private static final OperationStatus DELETED =
          new OperationStatus(true, "DELETED", StatusCode.SUCCESS);
  private static final OperationStatus NOT_FOUND =
          new OperationStatus(false, "NOT_FOUND", StatusCode.ERR_NOT_FOUND);

  private final String key;

  public DeleteOperationImpl(String k, OperationCallback cb) {
    super(cb);
    key = k;
    setAPIType(APIType.DELETE);
    setOperationType(OperationType.WRITE);
  }

  @Override
  public void handleLine(String line) {
    getLogger().debug("Delete of %s returned %s", key, line);
    /* ENABLE_REPLICATION if */
    if (line.equals("SWITCHOVER") || line.equals("REPL_SLAVE")) {
      receivedMoveOperations(line);
      return;
    }
    /* ENABLE_REPLICATION end */
    getCallback().receivedStatus(matchStatus(line, DELETED, NOT_FOUND));
    transitionState(OperationState.COMPLETE);
  }

  @Override
  public void initialize() {
    ByteBuffer b = ByteBuffer.allocate(
            KeyUtil.getKeyBytes(key).length + OVERHEAD);
    setArguments(b, "delete", key);
    b.flip();
    setBuffer(b);
  }

  public Collection<String> getKeys() {
    return Collections.singleton(key);
  }

  @Override
  public boolean isBulkOperation() {
    return false;
  }

  @Override
  public boolean isPipeOperation() {
    return false;
  }
}
