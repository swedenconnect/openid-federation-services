/*
 * Copyright 2024 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package se.digg.oidfed.resolver.tree.resolution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implements Breadth first search resolution of the federation.
 * Can be executed in parallel by injecting a multithreaded executor.
 *
 * @author Felix Hellman
 */
public class BFSExecution implements ExecutionStrategy {
  private final ExecutorService executor;
  private final List<Future<?>> pendingResults = new ArrayList<>();

  /**
   * @param executor to handle each iteration step
   */
  public BFSExecution(final ExecutorService executor) {
    this.executor = executor;
  }

  @Override
  public void execute(final Runnable runnable) {
    this.pendingResults.add(this.executor.submit(runnable));
  }

  @Override
  public void finalize(final Runnable runnable) {
    /*
     * Let this be a normal indexed for-loop
     * We want size to be evaluated in every loop to be sure that
     * futures that might be added to the list are waited for.
     */
    for (int i = 0; i < this.pendingResults.size(); i++) {
      try {
        this.pendingResults.get(i).get(5L, TimeUnit.SECONDS);
      }
      catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
      catch (ExecutionException | TimeoutException e) {
        throw new RuntimeException(e);
      }
    }

    this.pendingResults.clear();
    
    runnable.run();
  }
}
