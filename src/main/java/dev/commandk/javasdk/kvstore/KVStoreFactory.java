package dev.commandk.javasdk.kvstore;

import javax.annotation.Nonnull;

public interface KVStoreFactory<K, V> {
    @Nonnull KVStore<K, V> getStore();
}
