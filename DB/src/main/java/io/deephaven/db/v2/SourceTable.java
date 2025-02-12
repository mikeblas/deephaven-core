/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.v2;

import io.deephaven.base.verify.Assert;
import io.deephaven.base.verify.Require;
import io.deephaven.configuration.Configuration;
import io.deephaven.io.logger.Logger;
import io.deephaven.util.process.ProcessEnvironment;
import io.deephaven.db.tables.Table;
import io.deephaven.db.tables.TableDefinition;
import io.deephaven.db.tables.live.LiveTable;
import io.deephaven.db.tables.live.LiveTableRegistrar;
import io.deephaven.util.QueryConstants;
import io.deephaven.db.tables.utils.QueryPerformanceRecorder;
import io.deephaven.db.util.string.StringUtils;
import io.deephaven.db.v2.locations.TableDataException;
import io.deephaven.db.v2.locations.TableLocation;
import io.deephaven.db.v2.locations.TableLocationProvider;
import io.deephaven.db.v2.locations.TableLocationSubscriptionBuffer;
import io.deephaven.db.v2.sources.ColumnSource;
import io.deephaven.db.v2.sources.DeferredGroupingColumnSource;
import io.deephaven.db.v2.sources.EmptyToNullStringRegionedColumnSource;
import io.deephaven.db.v2.sources.LogicalClock;
import io.deephaven.db.v2.utils.Index;
import io.deephaven.util.annotations.TestUseOnly;
import io.deephaven.internal.log.LoggerFactory;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Basic uncoalesced table that only adds keys.
 */
public abstract class SourceTable extends RedefinableTable {

    /**
     * Static log instance for use in trace.
     */
    private static final Logger log = ProcessEnvironment.tryGet() == null ? LoggerFactory.getLogger(SourceTable.class) : ProcessEnvironment.getGlobalLog();

   /**
    * Component factory.  Mostly held for redefinitions.
    */
    final SourceTableComponentFactory componentFactory;

    /**
     * A column source manager to maintain our column sources and define how our index is updated.
     */
    final ColumnSourceManager columnSourceManager;

    /**
     * Location discovery.
     */
    final TableLocationProvider locationProvider;

    /**
     * Registration function for LiveTables that need to be refreshed.
     */
    final LiveTableRegistrar liveTableRegistrar;

    /**
     * Whether we've done our initial location fetch.
     */
    private volatile boolean locationsInitialized;

    /**
     * Whether we've done our initial location size fetches and initialized our index.
     */
    private volatile boolean locationSizesInitialized;

    /**
     * The index that backs this table, shared with all child tables.
     */
    private Index index;

    /**
     * The LiveTable object for refreshing locations and location sizes.
     */
    private LiveTable locationChangePoller;

    /**
     * Construct a new disk-backed table.
     *
     * @param tableDefinition    A TableDefinition
     * @param description        A human-readable description for this table
     * @param componentFactory   A component factory for creating column source managers
     * @param locationProvider   A TableLocationProvider, for use in discovering the locations that compose this table
     * @param liveTableRegistrar Callback for registering live tables for refreshes, null if this table is not live
     */
    SourceTable(@NotNull final TableDefinition tableDefinition,
                @NotNull final String description,
                @NotNull final SourceTableComponentFactory componentFactory,
                @NotNull final TableLocationProvider locationProvider,
                final LiveTableRegistrar liveTableRegistrar) {
        super(StringUtils.rewriteStringColumnTypes(tableDefinition), description);

        this.componentFactory = Require.neqNull(componentFactory, "componentFactory");
        this.locationProvider = Require.neqNull(locationProvider, "locationProvider");
        this.liveTableRegistrar = liveTableRegistrar;

        final boolean isLive = liveTableRegistrar != null;
        columnSourceManager = componentFactory.createColumnSourceManager(isLive, definition.getColumns() /* NB: this is the *re-written* definition passed to the super-class constructor. */);
        if (isLive) {
            // NB: There's no reason to start out trying to group, if this is a live table.
            columnSourceManager.disableGrouping();
        }

        setRefreshing(isLive);
        setAttribute(Table.ADD_ONLY_TABLE_ATTRIBUTE, Boolean.TRUE);
    }

    /**
     * Force this table to determine its initial state (available locations, size, index) if it hasn't already done so.
     */
    private void initialize() {
        initializeAvailableLocations();
        initializeLocationSizes();
    }

    /**
     * This is only for unit tests, at this time.
     */
    @TestUseOnly
    public final void refresh() {
        if (locationChangePoller != null) {
            locationChangePoller.refresh();
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected final void initializeAvailableLocations() {
        if (locationsInitialized) {
            return;
        }
        synchronized (this) {
            if (locationsInitialized) {
                return;
            }
            QueryPerformanceRecorder.withNugget(description + ".initializeAvailableLocations()", () -> {
                if (isRefreshing()) {
                    final TableLocationSubscriptionBuffer locationBuffer = new TableLocationSubscriptionBuffer(locationProvider);
                    maybeAddLocations(locationBuffer.processPending());
                    liveTableRegistrar.addTable(locationChangePoller = new LocationChangePoller(locationBuffer));
                } else {
                    locationProvider.refresh();
                    maybeAddLocations(locationProvider.getTableLocations());
                }
            });
            locationsInitialized = true;
        }
    }

    private void maybeAddLocations(@NotNull final Collection<TableLocation> locations) {
        if (locations.isEmpty()) {
            return;
        }
        filterLocations(locations).forEach(columnSourceManager::addLocation);
    }

    private void initializeLocationSizes() {
        Assert.assertion(locationsInitialized, "locationInitialized");
        if (locationSizesInitialized) {
            return;
        }
        synchronized (this) {
            if (locationSizesInitialized) {
                return;
            }
            QueryPerformanceRecorder.withNugget(description + ".initializeLocationSizes()", sizeForInstrumentation(), () -> {
                Assert.eqNull(index, "index");
                index = refreshLocationSizes();
                setAttribute(EMPTY_SOURCE_TABLE_ATTRIBUTE, index.empty());
                if (!isRefreshing()) {
                    return;
                }
                index.initializePreviousValue();
                final long currentClockValue = LogicalClock.DEFAULT.currentValue();
                setLastNotificationStep(LogicalClock.getState(currentClockValue) == LogicalClock.State.Updating
                        ? LogicalClock.getStep(currentClockValue) - 1
                        : LogicalClock.getStep(currentClockValue));
            });
            locationSizesInitialized = true;
        }
    }

    private Index refreshLocationSizes() {
        try {
            return columnSourceManager.refresh();
        } catch(Exception e) {
            throw new TableDataException("Error refreshing location sizes", e);
        }
    }

    private class LocationChangePoller extends InstrumentedLiveTable {

        private final TableLocationSubscriptionBuffer locationBuffer;

        private LocationChangePoller(@NotNull final TableLocationSubscriptionBuffer locationBuffer) {
            super(description + ".indexUpdateLiveTable");
            this.locationBuffer = locationBuffer;
        }

        @Override
        protected void instrumentedRefresh() {
            try {
                maybeAddLocations(locationBuffer.processPending());
                // NB: The availableLocationsLiveTable previously had functionality to notify
                //     "location listeners", but it was never used - resurrect from git history if needed.
                if (!locationSizesInitialized) {
                    // We don't want to start polling size changes until the initial Index has been computed.
                    return;
                }
                final boolean wasEmpty = index.empty();
                final Index added = refreshLocationSizes();
                if (added.size() == 0) {
                    return;
                }
                if (wasEmpty) {
                    setAttribute(EMPTY_SOURCE_TABLE_ATTRIBUTE, false);
                }
                index.insert(added);
                notifyListeners(added, Index.FACTORY.getEmptyIndex(), Index.FACTORY.getEmptyIndex());
            } catch (Exception e) {
                // Notify listeners to the SourceTable when we had an issue refreshing available locations.
                notifyListenersOnError(e, null);
            }
        }
    }

    /**
     * Hook to allow found locations to be filtered (e.g. according to a where-clause on the partitioning column of a
     * PartitionAwareSourceTable).
     * The default implementation returns its input unmolested.
     * @param foundLocations A Collection of TableLocations fetched from the table location provider, but not yet incorporated into the table
     * @return A sub-Collection of the input
     */
    protected Collection<TableLocation> filterLocations(@NotNull final Collection<TableLocation> foundLocations) {
        return foundLocations;
    }

    private static final boolean MAP_EMPTY_STRINGS_TO_NULL = Configuration.getInstance().getBooleanWithDefault("SourceTable.mapEmptyStringsToNull", false);

    private Map<String, ? extends ColumnSource> maybeReplaceColumnSources(@NotNull final Map<String, ? extends DeferredGroupingColumnSource> sources) {
        if (!MAP_EMPTY_STRINGS_TO_NULL) {
            return sources;
        }
        return sources.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                (e) -> {
                    final String name = e.getKey();
                    final DeferredGroupingColumnSource source = e.getValue();
                    if (getDefinition().getColumn(name).isPartitioning() || !CharSequence.class.isAssignableFrom(source.getType())) {
                        return source;
                    }
                    //noinspection unchecked
                    return new EmptyToNullStringRegionedColumnSource(source, () ->
                            log.info().append("EMPTY-TO-NULL-CONVERSION: ")
                                    .append(getDefinition().getNamespace()).append('.')
                                    .append(getDefinition().getName()).append('.')
                                    .append(name)
                                    .endl());
                },
                Assert::neverInvoked,
                LinkedHashMap::new));
    }

    @Override
    protected final QueryTable doCoalesce() {
        initialize();

        final ShiftAwareSwapListener swapListener = createSwapListenerIfRefreshing((final Logger log, final BaseTable parent) -> new ShiftAwareSwapListener(log, parent) {

            @Override
            public final void destroy() {
                // NB: We can't call super.destroy() because we don't want to try to remove ourselves from the
                // coalesced table (see override for removeUpdateListener), but we are probably not missing
                // anything by not having super.destroy() invoke its own super.destroy().
                removeUpdateListenerUncoalesced(this);
            }

            @Override
            public final void subscribeForUpdates() {
                listenForUpdatesUncoalesced(this);
            }
        });

        final Mutable<QueryTable> result = new MutableObject<>();
        initializeWithSnapshot("SourceTable.coalesce", swapListener, (usePrev, beforeClockValue) -> {
            final QueryTable resultTable = new QueryTable(definition, index, maybeReplaceColumnSources(columnSourceManager.getColumnSources()));
            copyAttributes(resultTable, CopyAttributeOperation.Coalesce);

            if (swapListener != null) {
                final ShiftAwareListenerImpl listener = new ShiftAwareListenerImpl("SourceTable.coalesce", this, resultTable) {

                    @Override
                    protected final void destroy() {
                        //NB: This implementation cannot call super.destroy() for the same reason as the swap listener
                        removeUpdateListenerUncoalesced(this);
                    }
                };
                swapListener.setListenerAndResult(listener, resultTable);
                resultTable.addParentReference(swapListener);
            }

            result.setValue(resultTable);
            return true;
        });

        return result.getValue();
    }

    protected static class QueryTableReference extends DeferredViewTable.TableReference {

        protected final SourceTable table;

        QueryTableReference(SourceTable table) {
            super(table);
            this.table = table;
        }

        @Override
        public long getSize() { return QueryConstants.NULL_LONG; }

        @Override
        public TableDefinition getDefinition() {
            return table.getDefinition();
        }

        @Override
        public Table get() {
            return table.coalesce();
        }
    }

    @Override
    protected void destroy() {
        super.destroy();
        if (liveTableRegistrar != null) {
            if (locationChangePoller != null) {
                liveTableRegistrar.removeTable(locationChangePoller);
            }
        }
    }
}
