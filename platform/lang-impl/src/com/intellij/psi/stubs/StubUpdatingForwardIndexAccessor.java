// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.ByteArraySequence;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.InputData;
import com.intellij.util.indexing.impl.InputDataDiffBuilder;
import com.intellij.util.indexing.impl.forward.ForwardIndexAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

class StubUpdatingForwardIndexAccessor implements ForwardIndexAccessor<Integer, SerializedStubTree> {
  private volatile UpdatableIndex<Integer, SerializedStubTree, FileContent> myIndex;

  @NotNull
  @Override
  public InputDataDiffBuilder<Integer, SerializedStubTree> getDiffBuilder(int inputId, @Nullable ByteArraySequence sequence) throws IOException {
    Map<Integer, SerializedStubTree> data;
    try {
      data = ProgressManager.getInstance().computeInNonCancelableSection(() -> myIndex.getIndexedFileData(inputId));
    }
    catch (StorageException e) {
      throw new IOException(e);
    }
    SerializedStubTree tree = ContainerUtil.isEmpty(data) ? null : ContainerUtil.getFirstItem(data.values());
    if (tree != null) {
      tree.restoreIndexedStubs();
    }
    return new StubCumulativeInputDiffBuilder(inputId, tree);
  }

  @Nullable
  @Override
  public ByteArraySequence serializeIndexedData(@NotNull InputData<Integer, SerializedStubTree> data) {
    return null;
  }

  void setIndex(@NotNull UpdatableIndex<Integer, SerializedStubTree, FileContent> index) {
    myIndex = index;
  }
}
