/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.v2.locations;

import io.deephaven.base.reference.WeakReferenceWrapper;
import io.deephaven.base.verify.Require;
import io.deephaven.util.type.NamedImplementation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * TableDataService implementation with support to filter the provided TableLocations.
 */
public class FilteredTableDataService extends AbstractTableDataService implements NamedImplementation {

    private final TableDataService serviceToFilter;
    private final LocationFilter locationFilter;

    @FunctionalInterface
    public interface LocationFilter {

        /**
         * Determine whether location should be visible via this service.
         *
         * @param location The location
         * @return True if the location should be visible, false otherwise
         */
        boolean accept(@NotNull TableLocation location);
    }

    /**
     * @param serviceToFilter The service that's being filtered.
     * @param locationFilter  The filter function.
     */
    @SuppressWarnings("WeakerAccess")
    public FilteredTableDataService(@NotNull final TableDataService serviceToFilter,
                                    @NotNull final LocationFilter locationFilter) {
        super("Filtered-" + Require.neqNull(serviceToFilter, "serviceToFilter").getName());
        this.serviceToFilter = Require.neqNull(serviceToFilter, "serviceToFilter");
        this.locationFilter = Require.neqNull(locationFilter, "locationFilter");
    }

    @Override
    public void reset() {
        super.reset();
        serviceToFilter.reset();
    }

    @Override
    public void reset(TableKey key) {
        super.reset(key);
        serviceToFilter.reset(key);
    }

    @Override
    protected @NotNull TableLocationProvider makeTableLocationProvider(@NotNull TableKey tableKey) {
        return new TableLocationProviderImpl(serviceToFilter.getTableLocationProvider(tableKey));
    }

    private class TableLocationProviderImpl implements TableLocationProvider {

        private final TableLocationProvider inputProvider;

        private final String implementationName;
        private final Map<Listener, FilteringListener> listeners = new WeakHashMap<>();

        private TableLocationProviderImpl(@NotNull final TableLocationProvider inputProvider) {
            this.inputProvider = inputProvider;
            implementationName = "Filtered-" + inputProvider.getImplementationName();
        }

        @Override
        public String getImplementationName() {
            return implementationName;
        }

        @Override
        public boolean supportsSubscriptions() {
            return inputProvider.supportsSubscriptions();
        }

        @Override
        public @NotNull CharSequence getNamespace() {
            return inputProvider.getNamespace();
        }

        @Override
        public @NotNull CharSequence getTableName() {
            return inputProvider.getTableName();
        }

        @Override
        public @NotNull TableType getTableType() {
            return inputProvider.getTableType();
        }

        @Override
        public void subscribe(@NotNull final Listener listener) {
            final FilteringListener filteringListener = new FilteringListener(listener);
            synchronized (listeners) {
                listeners.put(listener, filteringListener);
            }
            inputProvider.subscribe(filteringListener);
        }

        @Override
        public void unsubscribe(@NotNull final Listener listener) {
            final FilteringListener filteringListener;
            synchronized (listeners) {
                filteringListener = listeners.remove(listener);
            }
            if (filteringListener != null) {
                inputProvider.unsubscribe(filteringListener);
            }
        }

        @Override
        public void refresh() {
            inputProvider.refresh();
        }

        @Override
        public TableLocationProvider ensureInitialized() {
            inputProvider.ensureInitialized();
            return this;
        }

        @Override
        public  @NotNull Collection<TableLocation> getTableLocations() {
            return inputProvider.getTableLocations().stream().filter(locationFilter::accept).collect(Collectors.toSet());
        }

        @Nullable
        @Override
        public TableLocation getTableLocationIfPresent(@NotNull TableLocationKey tableLocationKey) {
            final TableLocation location = inputProvider.getTableLocationIfPresent(tableLocationKey);
            return location != null && locationFilter.accept(location) ? location : null;
        }

        @Override
        public String getName() {
            return FilteredTableDataService.this.getName();
        }
    }

    private class FilteringListener extends WeakReferenceWrapper<TableLocationProvider.Listener> implements TableLocationProvider.Listener {

        private FilteringListener(@NotNull final TableLocationProvider.Listener outputListener) {
            super(outputListener);
        }

        @Override
        public void handleTableLocation(@NotNull final TableLocation tableLocation) {
            final TableLocationProvider.Listener outputListener = getWrapped();
            // We can't try to clean up null listeners here, the underlying implementation may not allow concurrent unsubscribe operations.
            if (outputListener != null && locationFilter.accept(tableLocation)) {
                outputListener.handleTableLocation(tableLocation);
            }
        }

        @Override
        public void handleException(@NotNull TableDataException exception) {
            final TableLocationProvider.Listener outputListener = getWrapped();
            // See note in handleTableLocation.
            if (outputListener != null) {
                outputListener.handleException(exception);
            }
        }

        @Override
        public String toString() {
            return "FilteringListener{" + FilteredTableDataService.this + "}";
        }
    }

    @Override
    public String getImplementationName() {
        return "FilteredTableDataService";
    }

    @Override
    public String toString() {
        return getImplementationName() + '{' +
                (getName() != null ? "name=" + getName() + ", " : "") +
                "locationFilter=" + locationFilter +
                ", serviceToFilter=" + serviceToFilter +
                '}';
    }

    @Override
    public String describe() {
        return getImplementationName() + '{' +
                (getName() != null ? "name=" + getName() + ", " : "") +
                "locationFilter=" + locationFilter +
                ", serviceToFilter=" + serviceToFilter.describe() +
                '}';
    }
}
