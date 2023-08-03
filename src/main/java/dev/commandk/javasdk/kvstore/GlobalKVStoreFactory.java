package dev.commandk.javasdk.kvstore;

import javax.annotation.Nonnull;

public class GlobalKVStoreFactory<K, V> implements KVStoreFactory<K, V> {

    static InMemoryKVStore<?, ?> backingInMemoryKVStore = null;
    static final Object lock = new Object();


    @Override
    public @Nonnull KVStore<K, V> getStore() {
        synchronized (lock) {
            if (backingInMemoryKVStore == null) {
                backingInMemoryKVStore = new InMemoryKVStore<K, V>();
            }
        }
        return (InMemoryKVStore<K, V>) backingInMemoryKVStore;
    }
}
