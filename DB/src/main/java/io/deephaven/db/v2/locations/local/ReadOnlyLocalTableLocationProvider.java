package io.deephaven.db.v2.locations.local;

import io.deephaven.db.v2.locations.DeferredTableLocation;
import io.deephaven.db.v2.locations.TableKey;
import io.deephaven.db.v2.locations.TableLocation;
import io.deephaven.db.v2.locations.TableLocationKey;
import io.deephaven.db.v2.locations.util.TableDataRefreshService;
import io.deephaven.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Location provider for read-only table locations.
 */
public class ReadOnlyLocalTableLocationProvider extends LocalTableLocationProvider {

    private static final String PARQUET_FILE_NAME = "table.parquet";

    private final TableDataRefreshService refreshService;

    private TableDataRefreshService.CancellableSubscriptionToken subscriptionToken;

    public ReadOnlyLocalTableLocationProvider(@NotNull final TableKey tableKey,
                                              @NotNull final Scanner scanner,
                                              final boolean supportsSubscriptions,
                                              @NotNull final TableDataRefreshService refreshService) {
        super(tableKey, scanner, supportsSubscriptions);
        this.refreshService = refreshService;
    }

    @Override
    public String getImplementationName() {
        return "ReadOnlyLocalTableLocationProvider";
    }

    @Override
    public void refresh() {
        scanner.scanAll(this::handleTableLocationKey);
        setInitialized();
    }

    @Override
    protected void activateUnderlyingDataSource() {
        subscriptionToken = refreshService.scheduleTableLocationProviderRefresh(this);
    }

    @Override
    protected void deactivateUnderlyingDataSource() {
        if (subscriptionToken != null) {
            subscriptionToken.cancel();
            subscriptionToken = null;
        }
    }

    @Override
    protected <T> boolean matchSubscriptionToken(final T token) {
        return token == subscriptionToken;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    protected final TableLocation makeTableLocation(@NotNull final TableLocationKey locationKey) {
        final TableLocationMetadataIndex.TableLocationSnapshot snapshot = maybeGetSnapshot(locationKey);
        if (snapshot == null) {
            return new DeferredTableLocation.DataDriven<>(getTableKey(), locationKey, this::makeDataDrivenLocation);
        }
        if (snapshot.getFormat() == TableLocation.Format.PARQUET) {
            return new DeferredTableLocation.SnapshotDriven<>(getTableKey(), snapshot, this::makeSnapshottedParquetLocation);
        }
        throw new UnsupportedOperationException(this + ": Unrecognized format " + snapshot.getFormat() + " for snapshotted location " + locationKey);
    }

    private TableLocation makeSnapshottedParquetLocation(@NotNull final TableKey tableKey, @NotNull final TableLocationKey tableLocationKey) {
        return new ReadOnlyParquetTableLocation(tableKey, tableLocationKey, new File(scanner.computeLocationDirectory(tableKey, tableLocationKey), PARQUET_FILE_NAME), false);
    }

    private TableLocation makeDataDrivenLocation(@NotNull final TableKey tableKey, @NotNull final TableLocationKey tableLocationKey) {
        final File directory = scanner.computeLocationDirectory(tableKey, tableLocationKey);
        final File parquetFile = new File(directory, PARQUET_FILE_NAME);
        if (Utils.fileExistsPrivileged(parquetFile)) {
            return new ReadOnlyParquetTableLocation(tableKey, tableLocationKey, parquetFile, supportsSubscriptions());
        } else {
            throw new UnsupportedOperationException(this + ": Unrecognized data format in location " + tableLocationKey);
        }
    }

    private TableLocationMetadataIndex.TableLocationSnapshot maybeGetSnapshot(@NotNull final TableLocationKey tableLocationKey) {
        if (supportsSubscriptions()) {
            return null;
        }
        final TableLocationMetadataIndex metadataIndex = scanner.getMetadataIndex();
        if (metadataIndex == null) {
            return null;
        }
        return metadataIndex.getTableLocationSnapshot(tableLocationKey);
    }
}
