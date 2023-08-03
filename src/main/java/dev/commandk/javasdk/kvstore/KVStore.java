package dev.commandk.javasdk.kvstore;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public interface KVStore<K, V> {
    @Nonnull Optional<V> get(@Nonnull K key);
    void set(@Nonnull K key, @Nonnull V value);
}

class InMemoryKVStore<K, V> implements KVStore<K, V> {
    private final ConcurrentHashMap<K, V> kvStorageBackingMap = new ConcurrentHashMap<K, V>();

    @Override
    public @Nonnull Optional<V> get(@Nonnull K key) {
        V value = kvStorageBackingMap.compute(key, (K k, V v) -> v);
        return value != null ? Optional.of(value) : Optional.empty();
    }

    @Override
    public void set(@Nonnull K key, @Nonnull V value) {
        if(value!=null) kvStorageBackingMap.put(key, value);
        else kvStorageBackingMap.remove(key);
    }
}