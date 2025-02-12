/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.v2.sources.regioned;

import io.deephaven.base.verify.Require;
import io.deephaven.db.tables.ColumnDefinition;
import io.deephaven.db.tables.libs.StringSet;
import io.deephaven.db.tables.utils.DBDateTime;
import io.deephaven.db.v2.ColumnSourceManager;
import io.deephaven.util.codec.*;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.Externalizable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory that assembles modular components for regioned source tables.
 */
public class RegionedTableComponentFactoryImpl implements RegionedTableComponentFactory {

    private static final Map<Class<?>, Supplier<RegionedColumnSource<?>>> SIMPLE_DATA_TYPE_TO_REGIONED_COLUMN_SOURCE_SUPPLIER;

    static {
        Map<Class<?>, Supplier<RegionedColumnSource<?>>> typeToSupplier = new HashMap<>();
        typeToSupplier.put(Byte.class, RegionedColumnSourceByte.AsValues::new);
        typeToSupplier.put(Character.class, RegionedColumnSourceChar.AsValues::new);
        typeToSupplier.put(Double.class, RegionedColumnSourceDouble.AsValues::new);
        typeToSupplier.put(Float.class, RegionedColumnSourceFloat.AsValues::new);
        typeToSupplier.put(Integer.class, RegionedColumnSourceInt.AsValues::new);
        typeToSupplier.put(Long.class, RegionedColumnSourceLong.AsValues::new);
        typeToSupplier.put(Short.class, RegionedColumnSourceShort.AsValues::new);
        typeToSupplier.put(Boolean.class, RegionedColumnSourceBoolean::new);
        typeToSupplier.put(DBDateTime.class, RegionedColumnSourceDBDateTime::new);
        SIMPLE_DATA_TYPE_TO_REGIONED_COLUMN_SOURCE_SUPPLIER = Collections.unmodifiableMap(typeToSupplier);
    }

    public static final RegionedTableComponentFactory INSTANCE = new RegionedTableComponentFactoryImpl();

    private RegionedTableComponentFactoryImpl() {
    }

    @Override
    public ColumnSourceManager createColumnSourceManager(final boolean isRefreshing, @NotNull final ColumnDefinition<?>... columnDefinitions) {
        return new RegionedColumnSourceManager(isRefreshing, this, columnDefinitions);
    }

    /**
     * Create a new {@link RegionedColumnSource} appropriate to implement the supplied {@link ColumnDefinition}.
     * <br><b>Note:</b> This implementation currently lets arrays fall into the catch-all (Serializable) case.
     * <br><b>Note:</b> There is no handling for Enum(Set) assignables.  No existing tables should have legacy Enum(Set) columns.
     *
     * @param columnDefinition The column definition
     * @param <DATA_TYPE>      The data type of the column
     * @return A new RegionedColumnSource.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <DATA_TYPE> RegionedColumnSource<DATA_TYPE> createRegionedColumnSource(@NotNull final ColumnDefinition<DATA_TYPE> columnDefinition) {
        Class<DATA_TYPE> dataType = TypeUtils.getBoxedType(columnDefinition.getDataType());

        if (columnDefinition.isPartitioning()) {
            Require.eq(dataType, "dataType", String.class);
            Require.eqNull(columnDefinition.getSymbolTableType(), "columnDefinition.getSymbolTableType()");
            return (RegionedColumnSource<DATA_TYPE>) new RegionedColumnSourcePartitioning();
        }

        final Supplier<RegionedColumnSource<?>> simpleImplementationSupplier = SIMPLE_DATA_TYPE_TO_REGIONED_COLUMN_SOURCE_SUPPLIER.get(dataType);
        if (simpleImplementationSupplier != null) {
            return (RegionedColumnSource<DATA_TYPE>) simpleImplementationSupplier.get();
        }

        try {
            if (CharSequence.class.isAssignableFrom(dataType)) {
                switch (columnDefinition.getSymbolTableType()) {
                    case NONE:
                        return new RegionedColumnSourceObject.AsValues<>(dataType,
                                RegionedTableComponentFactory.getStringDecoder(dataType, columnDefinition));
                    case COLUMN_LOCATION:
                        return new RegionedColumnSourceObjectWithDictionary<>(dataType,
                                RegionedTableComponentFactory.getStringDecoder(dataType, columnDefinition));
                    default:
                        throw new UnsupportedOperationException("Unknown symbol type " + columnDefinition.getSymbolTableType() +
                                " in column definition " + columnDefinition);
                }
            } else if (StringSet.class.isAssignableFrom(dataType)) {
                return (RegionedColumnSource<DATA_TYPE>) new RegionedColumnSourceStringSet(
                        RegionedTableComponentFactory.getStringDecoder(String.class, columnDefinition));
            } else {
                ObjectDecoder<DATA_TYPE> decoder = null;

                if (columnDefinition.isFixedWidthObjectType()) {
                    decoder = CodecCache.DEFAULT.getCodec(columnDefinition.getObjectCodecClass(), columnDefinition.getObjectCodecArguments());
                    if (decoder != null) {
                        decoder.checkWidth(columnDefinition.getObjectWidth());
                    }
                } else {
                    switch (columnDefinition.getObjectCodecType()) {
                        case DEFAULT:
                            if (Externalizable.class.isAssignableFrom(dataType)) {
                                decoder = CodecCache.DEFAULT.getCodec(ExternalizableCodec.class.getName(), dataType.getName());
                                break;
                            }
                            // Fall through
                        case SERIALIZABLE:
                            decoder = SerializableCodec.create();
                            break;
                        case EXTERNALIZABLE:
                            decoder = CodecCache.DEFAULT.getCodec(ExternalizableCodec.class.getName(), dataType.getName());
                            break;
                        case CLASS:
                            decoder = CodecCache.DEFAULT.getCodec(columnDefinition.getObjectCodecClass(), columnDefinition.getObjectCodecArguments());
                            break;
                    }
                }

                if (decoder != null) {
                    return new RegionedColumnSourceObject.AsValues<>(dataType, decoder);
                } else {
                    throw new UnsupportedOperationException("Unable to supply a column source for " + columnDefinition);
                }
            }
        } catch (IllegalArgumentException except) {
            throw new UnsupportedOperationException("Can't create column for " + dataType + " in column definition " + columnDefinition, except);
        }
    }

}
