/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.content;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;

import java.util.ArrayList;
import java.util.List;

public class IndexedAggregatedContent extends AbstractAggregatedContent implements ObjectStoreAwareAggregatedContent {

  private static final long serialVersionUID = -4606827493355267037L;
  private transient ObjectStore<TypedValue> objectStore;

  private String aggregatorKey;
  private int maxSize;
  private List<String> unorderedElementKeys;
  private List<String> orderedElementKeys;

  public IndexedAggregatedContent(String aggregatorKey, int maxSize, ObjectStore<TypedValue> objectStore) {
    this.aggregatorKey = aggregatorKey;
    this.maxSize = maxSize;
    this.unorderedElementKeys = new ArrayList<>();
    this.orderedElementKeys = new ArrayList<>();
    this.objectStore = objectStore;
  }

  @Override
  public void setObjectStore(ObjectStore<TypedValue> objectStore) {
    this.objectStore = objectStore;
  }

  @Override
  public void add(TypedValue newElement, Long timestamp) {
    String newElementKey = generateNewUnorderedElementKey(unorderedElementKeys.size());
    try {
      objectStore.store(newElementKey, newElement);
    } catch (ObjectStoreException e) {
      //TODO: FIX THIS!!!
      throw new MuleRuntimeException(e);
    }
    this.unorderedElementKeys.add(newElementKey);
    updateTimes(timestamp);
  }

  @Override
  public void add(TypedValue newElement, Long timestamp, int sequenceNumber) {
    String newElementKey = generateNewOrderedElementKey(sequenceNumber);
    try {
      objectStore.store(newElementKey, newElement);
    } catch (ObjectStoreException e) {
      //TODO: FIX THIS!!!
      throw new MuleRuntimeException(e);
    }
    this.orderedElementKeys.add(newElementKey);
    updateTimes(timestamp);
  }

  @Override
  public List<TypedValue> getAggregatedElements() {
    List<TypedValue> allValues = new ArrayList<>();
    List<String> sortedOrderedElementKeys = orderedElementKeys.stream().sorted().collect(toList());
    try {
      for (String key : sortedOrderedElementKeys) {
        allValues.add(objectStore.retrieve(key));
      }
      for (String key : unorderedElementKeys) {
        allValues.add(objectStore.retrieve(key));
      }
    } catch (ObjectStoreException e) {
      //TODO: FIX THIS!!!
      throw new MuleRuntimeException(e);
    }
    return allValues;
  }

  @Override
  public boolean isComplete() {
    return maxSize == unorderedElementKeys.size() + orderedElementKeys.size();
  }

  private String generateNewUnorderedElementKey(int nextIndex) {
    return format("%s.unordered.values.%d", aggregatorKey, nextIndex);
  }

  private String generateNewOrderedElementKey(int index) {
    return format("%s.ordered.values.%d", aggregatorKey, index);
  }
}
