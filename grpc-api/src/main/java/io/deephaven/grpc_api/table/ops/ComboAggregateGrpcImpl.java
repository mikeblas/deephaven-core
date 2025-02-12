package io.deephaven.grpc_api.table.ops;

import io.deephaven.base.verify.Assert;
import io.deephaven.datastructures.util.CollectionUtil;
import io.deephaven.db.tables.DataColumn;
import io.deephaven.db.tables.Table;
import io.deephaven.db.tables.select.SelectColumnFactory;
import io.deephaven.db.v2.by.ComboAggregateFactory;
import io.deephaven.db.v2.select.SelectColumn;
import io.deephaven.grpc_api.session.SessionState;
import io.deephaven.grpc_api.table.validation.ColumnExpressionValidator;
import io.deephaven.proto.backplane.grpc.BatchTableRequest;
import io.deephaven.proto.backplane.grpc.ComboAggregateRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Singleton
public class ComboAggregateGrpcImpl extends GrpcTableOperation<ComboAggregateRequest> {

    @Inject
    public ComboAggregateGrpcImpl() {
        super(BatchTableRequest.Operation::getComboAggregate, ComboAggregateRequest::getResultId, ComboAggregateRequest::getSourceId);
    }

    @Override
    public Table create(final ComboAggregateRequest request, final List<SessionState.ExportObject<Table>> sourceTables) {
        Assert.eq(sourceTables.size(), "sourceTables.size()", 1);

        final Table parent = sourceTables.get(0).get();
        final String[] groupBySpecs = request.getGroupByColumnsList().toArray(CollectionUtil.ZERO_LENGTH_STRING_ARRAY);
        final SelectColumn[] groupByColumns = SelectColumnFactory.getExpressions(groupBySpecs);
        ColumnExpressionValidator.validateColumnExpressions(groupByColumns, groupBySpecs, parent);

        final Table result;
        if (!request.getForceCombo() && request.getAggregatesCount() == 1
                && request.getAggregates(0).getType() != ComboAggregateRequest.AggType.Percentile
                && request.getAggregates(0).getMatchPairsCount() == 0) {
            // This is a special case with a special operator that can be invoked right off of the table api.
            result = singleAggregateHelper(parent, groupByColumns, request.getAggregates(0));
        } else {
            result = comboAggregateHelper(parent, groupByColumns, request.getAggregatesList());
        }
        return result;
    }

    private static Table singleAggregateHelper(final Table parent, final SelectColumn[] groupByColumns, final ComboAggregateRequest.Aggregate aggregate) {
        switch (aggregate.getType()) {
            case Sum:
                return parent.sumBy(groupByColumns);
            case AbsSum:
                return parent.absSumBy(groupByColumns);
            case Array:
                return parent.by(groupByColumns);
            case Avg:
                return parent.avgBy(groupByColumns);
            case Count:
                return parent.countBy(aggregate.getColumnName(), groupByColumns);
            case First:
                return parent.firstBy(groupByColumns);
            case Last:
                return parent.lastBy(groupByColumns);
            case Min:
                return parent.minBy(groupByColumns);
            case Max:
                return parent.maxBy(groupByColumns);
            case Median:
                return parent.medianBy(groupByColumns);
            case Std:
                return parent.stdBy(groupByColumns);
            case Var:
                return parent.varBy(groupByColumns);
            case WeightedAvg:
                return parent.wavgBy(aggregate.getColumnName(), groupByColumns);
            default:
                throw new UnsupportedOperationException("Unsupported aggregate: " + aggregate.getType());
        }
    }

    private static Table comboAggregateHelper(final Table parent, final SelectColumn[] groupByColumns, final List<ComboAggregateRequest.Aggregate> aggregates) {
        final Set<String> groupByColumnSet = Arrays.stream(groupByColumns).map(SelectColumn::getName).collect(Collectors.toSet());

        final ComboAggregateFactory.ComboBy[] comboBy =
                new ComboAggregateFactory.ComboBy[aggregates.size()];

        for (int i = 0; i < aggregates.size(); i++) {
            final ComboAggregateRequest.Aggregate agg = aggregates.get(i);

            final String[] matchPairs;
            if (agg.getMatchPairsCount() == 0) {
                // if not specified, we apply the aggregate to all columns not "otherwise involved"
                matchPairs = Arrays.stream(parent.getColumns())
                        .map(DataColumn::getName)
                        .filter(n -> !(groupByColumnSet.contains(n) || (agg.getType() == ComboAggregateRequest.AggType.WeightedAvg && agg.getColumnName().equals(n))))
                        .toArray(String[]::new);
            } else {
                matchPairs = agg.getMatchPairsList().toArray(CollectionUtil.ZERO_LENGTH_STRING_ARRAY);
                final SelectColumn[] matchPairExpressions = SelectColumnFactory.getExpressions(matchPairs);
                ColumnExpressionValidator.validateColumnExpressions(matchPairExpressions, matchPairs, parent);
            }

            final Supplier<ComboAggregateFactory.ComboBy> comboMapper = () -> {
                switch (agg.getType()) {
                    case Sum:
                        return ComboAggregateFactory.AggSum(matchPairs);
                    case AbsSum:
                        return ComboAggregateFactory.AggAbsSum(matchPairs);
                    case Array:
                        return ComboAggregateFactory.AggArray(matchPairs);
                    case Avg:
                        return ComboAggregateFactory.AggAvg(matchPairs);
                    case Count:
                        return ComboAggregateFactory.AggCount(agg.getColumnName());
                    case First:
                        return ComboAggregateFactory.AggFirst(matchPairs);
                    case Last:
                        return ComboAggregateFactory.AggLast(matchPairs);
                    case Min:
                        return ComboAggregateFactory.AggMin(matchPairs);
                    case Max:
                        return ComboAggregateFactory.AggMax(matchPairs);
                    case Median:
                        return ComboAggregateFactory.AggMed(matchPairs);
                    case Percentile:
                        return ComboAggregateFactory.AggPct(agg.getPercentile(), agg.getAvgMedian(), matchPairs);
                    case Std:
                        return ComboAggregateFactory.AggStd(matchPairs);
                    case Var:
                        return ComboAggregateFactory.AggVar(matchPairs);
                    case WeightedAvg:
                        return ComboAggregateFactory.AggWAvg(agg.getColumnName(), matchPairs);
                    default:
                        throw new UnsupportedOperationException("Unsupported aggregate: " + agg.getType());
                }
            };

            comboBy[i] = comboMapper.get();
        }

        return parent.by(ComboAggregateFactory.AggCombo(comboBy), groupByColumns);
    }
}
