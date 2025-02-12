/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.v2.locations;

import io.deephaven.hash.KeyedObjectHashSet;
import io.deephaven.base.verify.Require;
import io.deephaven.db.util.Formatter;
import io.deephaven.util.SafeCloseable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Routing TableDataService that applies a selector function to pick service(s) for each request.
 * It is assumed that each service will provide access to a non-overlapping set of table locations for any table key.
 */
public class CompositeTableDataService extends AbstractTableDataService {

    private final ServiceSelector serviceSelector;

    public interface ServiceSelector {
        TableDataService[] call(TableKey tableKey);
        void resetServices();
        void resetServices(TableKey key);

        /**
         * Like toString, but with more detail.
         * @return a description string
         */
        String describe();
    }

    /**
     * @param name            optional name for this service
     * @param serviceSelector Function to map a table key to a set of services that should be queried.
     */
    @SuppressWarnings("WeakerAccess")
    public CompositeTableDataService(@NotNull String name, @NotNull final ServiceSelector serviceSelector) {
        super(name);
        this.serviceSelector = Require.neqNull(serviceSelector, "serviceSelector");
    }

    @Override
    public void reset() {
        super.reset();
        serviceSelector.resetServices();
    }

    @Override
    public void reset(TableKey key) {
        super.reset(key);
        serviceSelector.resetServices(key);
    }

    @Override
    protected @NotNull TableLocationProvider makeTableLocationProvider(@NotNull final TableKey tableKey) {
        final TableDataService services[] = serviceSelector.call(tableKey);
        if (services == null || services.length == 0) {
            throw new TableDataException("No services found for " + tableKey + " in " + serviceSelector);
        }
        if (services.length == 1) {
            return services[0].getTableLocationProvider(tableKey);
        }
        return new TableLocationProviderImpl(services, tableKey);
    }

    private class TableLocationProviderImpl implements TableLocationProvider {

        private final TableKey tableKey;

        private final List<TableLocationProvider> inputProviders;
        private final String implementationName;

        private TableLocationProviderImpl(@NotNull final TableDataService[] inputServices, @NotNull final TableKey tableKey) {
            this.tableKey = TableLookupKey.getImmutableKey(tableKey);
            inputProviders = Arrays.stream(inputServices).map(s -> s.getTableLocationProvider(this.tableKey)).collect(Collectors.toList());
            implementationName = "Composite-" + inputProviders.toString();
        }

        @Override
        public String getImplementationName() {
            return implementationName;
        }

        @Override
        public @NotNull CharSequence getNamespace() {
            return tableKey.getNamespace();
        }

        @Override
        public @NotNull CharSequence getTableName() {
            return tableKey.getTableName();
        }

        @Override
        public @NotNull TableType getTableType() {
            return tableKey.getTableType();
        }

        @Override
        public boolean supportsSubscriptions() {
            return inputProviders.stream().anyMatch(TableLocationProvider::supportsSubscriptions);
        }

        @Override
        public void subscribe(@NotNull final Listener listener) {
            inputProviders.forEach(p -> {
                if (p.supportsSubscriptions()) {
                    p.subscribe(listener);
                } else {
                    p.refresh();
                    p.getTableLocations().forEach(listener::handleTableLocation);
                }
            });
        }

        @Override
        public void unsubscribe(@NotNull final Listener listener) {
            inputProviders.forEach(p -> {
                if (p.supportsSubscriptions()) {
                    p.unsubscribe(listener);
                }
            });
        }

        @Override
        public void refresh() {
            inputProviders.forEach(TableLocationProvider::refresh);
        }

        @Override
        public TableLocationProvider ensureInitialized() {
            inputProviders.forEach(TableLocationProvider::ensureInitialized);
            return this;
        }

        @Override
        public  @NotNull Collection<TableLocation> getTableLocations() {
            final Set<TableLocation> locations = new KeyedObjectHashSet<>(TableLocationKey.getKeyedObjectKey());

            try (final SafeCloseable ignored = CompositeTableDataServiceConsistencyMonitor.INSTANCE.start()) {
                final TableLocation duplicateLocation = inputProviders.stream().map(TableLocationProvider::getTableLocations)
                        .flatMap(Collection::stream)
                        .filter(x -> !locations.add(x))
                        .findFirst().orElse(null);
                if (duplicateLocation == null) {
                    return Collections.unmodifiableCollection(locations);
                }

                // bad news; there is a duplicate location
                // Look this key up again to find out which elements accept it (and it should throw an exception)
                getTableLocationIfPresent(duplicateLocation);
                // throw a backup exception (for the unlikely case where getTableLocationIfPresent doesn't throw.
                throw new TableDataException("Data Routing Configuration error: TableDataService elements overlap at location " +
                        duplicateLocation.toGenericString() +
                        ". Full TableDataService configuration:\n" +
                        Formatter.formatTableDataService(CompositeTableDataService.this.toString()));
            }
        }

        @Override
        public @Nullable TableLocation getTableLocationIfPresent(@NotNull final TableLocationKey tableLocationKey) {
            // hang onto the first location and provider, so we can report well on any duplicates
            TableLocation location = null;
            TableLocationProvider provider = null;

            try (final SafeCloseable ignored = CompositeTableDataServiceConsistencyMonitor.INSTANCE.start()) {
                for (final TableLocationProvider tlp : inputProviders) {
                    final TableLocation candidateLocation = tlp.getTableLocationIfPresent(tableLocationKey);
                    if (candidateLocation != null) {
                        if (location != null) {
                            throw new TableDataException("Data Routing Configuration error: TableDataService elements " + provider.getName() +
                                    " and " + tlp.getName() + " overlap at location " + location.toGenericString() +
                                    ". Full TableDataService configuration:\n" +
                                    Formatter.formatTableDataService(CompositeTableDataService.this.toString()));
                        }
                        location = candidateLocation;
                        provider = tlp;
                    }
                }
            }
            return location;
        }
    }

    @Override
    public String toString() {
        return "CompositeTableDataService{" +
                (getName() == null ? "" : "name=" + getName() + ", ") +
                "serviceSelector=" + serviceSelector +
                '}';
    }

    @Override
    public String describe() {
        return "CompositeTableDataService{" +
                (getName() == null ? "" : "name=" + getName() + ", ") +
                "serviceSelector=" + serviceSelector.describe() +
                '}';
    }
}
