/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.v2.select;

import io.deephaven.base.verify.Assert;
import io.deephaven.base.verify.AssertionFailure;
import io.deephaven.configuration.Configuration;
import io.deephaven.compilertools.CompilerTools;
import io.deephaven.db.tables.ColumnDefinition;
import io.deephaven.db.tables.DefaultColumnDefinition;
import io.deephaven.db.tables.Table;
import io.deephaven.db.tables.lang.DBLanguageFunctionUtil;
import io.deephaven.db.tables.lang.DBLanguageParser.QueryLanguageParseException;
import io.deephaven.db.tables.libs.QueryLibrary;
import io.deephaven.db.tables.select.QueryScope;
import io.deephaven.db.tables.utils.*;
import io.deephaven.db.v2.utils.codegen.TypeAnalyzer;
import io.deephaven.util.QueryConstants;
import io.deephaven.util.type.TypeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static io.deephaven.db.v2.select.FormulaTestUtil.*;
import static junit.framework.TestCase.*;

/**
 * Create a simple formula column.
 */
@SuppressWarnings("SameParameterValue")
@RunWith(Parameterized.class)
public class TestFormulaColumn {
    @Parameterized.Parameters(name = "useKernelFormulasProperty = {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { new Object[] { false }, new Object[] { true } });
    }

    private final Table testDataTable;
    private final Map<String, ColumnDefinition> availableColumns;
    private boolean compilerToolsLogEnabledInitial = false;
    private final boolean useKernelFormulas;
    private boolean kernelFormulasSavedValue;

    static {
        setUpQueryScope();
    }

    public TestFormulaColumn(boolean useKernelFormulas) {
        this.useKernelFormulas = useKernelFormulas;
        testDataTable = getTestDataTable();
        availableColumns = testDataTable.getDefinition().getColumnNameMap();
    }

    @Before
    public void setUp() throws Exception {
        kernelFormulasSavedValue = DhFormulaColumn.useKernelFormulasProperty;
        DhFormulaColumn.useKernelFormulasProperty = useKernelFormulas;
        compilerToolsLogEnabledInitial = CompilerTools.setLogEnabled(true);

        setUpQueryLibrary();
    }

    @After
    public void tearDown() throws Exception {
        CompilerTools.setLogEnabled(compilerToolsLogEnabledInitial);

        QueryLibrary.resetCurrent();
        DhFormulaColumn.useKernelFormulasProperty = kernelFormulasSavedValue;
    }

    //---------- TESTS

    @SuppressWarnings("RedundantCast")
    @Test
    public void testSimpleLiterals() {
        check("(byte) 42", (byte) 42);
        check("(short) 42", (short) 42);
        check("(char) 42", (char) 42);
        check("(int) 42", (int) 42);
        check("(long) 42", (long) 42);
        check("(float) 42", (float) 42);
        check("(double) 42", (double) 42);
        check("true", true);
    }

    @Test
    public void testTimestamp() {
        check("'2019-04-11T09:30 NY'", new DBDateTime(1554989400000000000L));
    }

    @Test
    public void testPositional() {
        checkPrimitive(0, "i", 0);
        checkPrimitive(1, "i", 1);
        checkPrimitive(2, "i", 2);

        checkPrimitive(0, "ii", 0L);
        checkPrimitive(1, "ii", 1L);
        checkPrimitive(2, "ii", 2L);
    }

    @SuppressWarnings("RedundantCast")
    @Test
    public void testVariables() {
        checkPrimitive(0, "ByteCol", (byte) BASE_VALUES[0]);
        checkPrimitive(0, "ShortCol", (short) BASE_VALUES[0]);
        checkPrimitive(0, "CharCol", (char) BASE_VALUES[0]);
        checkPrimitive(0, "IntCol", (int) BASE_VALUES[0]);
        checkPrimitive(0, "LongCol", (long) BASE_VALUES[0]);
        checkPrimitive(0, "FloatCol", (float) BASE_VALUES[0]);
        checkPrimitive(0, "DoubleCol", (double) BASE_VALUES[0]);
        check(0, "BooleanCol", false);

        checkPrimitive(1, "ByteCol", (byte) BASE_VALUES[1]);
        checkPrimitive(1, "ShortCol", (short) BASE_VALUES[1]);
        checkPrimitive(1, "CharCol", (char) BASE_VALUES[1]);
        checkPrimitive(1, "IntCol", (int) BASE_VALUES[1]);
        checkPrimitive(1, "LongCol", (long) BASE_VALUES[1]);
        checkPrimitive(1, "FloatCol", (float) BASE_VALUES[1]);
        checkPrimitive(1, "DoubleCol", (double) BASE_VALUES[1]);
        check(1, "BooleanCol", true);
    }

    /**
     * FormulaColumns never return boxed types (except Boolean). If the formula itself evaluates to a boxed type,
     * it will be unboxed.
     */
    @Test
    public void testReturnUnboxedType() {
        for(int row=0;row<testDataTable.size();row++) {
            checkPrimitive(row, "new Byte(ByteCol)", DBLanguageFunctionUtil.byteCast(BASE_VALUES[row]));
            checkPrimitive(row, "new Short(ShortCol)", DBLanguageFunctionUtil.shortCast(BASE_VALUES[row]));
            checkPrimitive(row, "new Character(CharCol)", DBLanguageFunctionUtil.charCast(BASE_VALUES[row]));
            checkPrimitive(row, "new Integer(IntCol)", DBLanguageFunctionUtil.intCast(BASE_VALUES[row]));
            checkPrimitive(row, "new Long(LongCol)", DBLanguageFunctionUtil.longCast(BASE_VALUES[row]));
            checkPrimitive(row, "new Float(FloatCol)", DBLanguageFunctionUtil.floatCast(BASE_VALUES[row]));
            checkPrimitive(row, "new Double(DoubleCol)", DBLanguageFunctionUtil.doubleCast(BASE_VALUES[row]));
        }
    }

    @Test
    public void testIsNull() {
        check(NULL_ROW_INDEX, "isNull(ByteCol)", true);
        check(NULL_ROW_INDEX, "isNull(ShortCol)", true);
        check(NULL_ROW_INDEX, "isNull(CharCol)", true);
        check(NULL_ROW_INDEX, "isNull(IntCol)", true);
        check(NULL_ROW_INDEX, "isNull(LongCol)", true);
        check(NULL_ROW_INDEX, "isNull(FloatCol)", true);
        check(NULL_ROW_INDEX, "isNull(DoubleCol)", true);
        check(NULL_ROW_INDEX, "isNull(BooleanCol)", true);

        check(ZERO_ROW_INDEX, "isNull(ByteCol)", false);
        check(ZERO_ROW_INDEX, "isNull(ShortCol)", false);
        check(ZERO_ROW_INDEX, "isNull(CharCol)", false);
        check(ZERO_ROW_INDEX, "isNull(IntCol)", false);
        check(ZERO_ROW_INDEX, "isNull(LongCol)", false);
        check(ZERO_ROW_INDEX, "isNull(FloatCol)", false);
        check(ZERO_ROW_INDEX, "isNull(DoubleCol)", false);
        check(ZERO_ROW_INDEX, "isNull(BooleanCol)", false);
    }

    @Test
    public void testArrayEvaluation() {
        check(1, "ByteCol_[i - 1]", (byte)BASE_VALUES[0]);
        check(1, "ShortCol_[i - 1]", (short)BASE_VALUES[0]);
        check(1, "CharCol_[i - 1]", (char)BASE_VALUES[0]);
        check(1, "IntCol_[i - 1]", (int)BASE_VALUES[0]);
        check(1, "LongCol_[i - 1]", (long)BASE_VALUES[0]);
        check(1, "FloatCol_[i - 1]", (float)BASE_VALUES[0]);
        check(1, "DoubleCol_[i - 1]", (double)BASE_VALUES[0]);
        check(1, "BooleanCol_[i - 1]", false);

        check(1, "ByteCol_[i]", (byte)BASE_VALUES[1]);
        check(1, "ShortCol_[i]", (short)BASE_VALUES[1]);
        check(1, "CharCol_[i]", (char)BASE_VALUES[1]);
        check(1, "IntCol_[i]", (int)BASE_VALUES[1]);
        check(1, "LongCol_[i]", (long)BASE_VALUES[1]);
        check(1, "FloatCol_[i]", (float)BASE_VALUES[1]);
        check(1, "DoubleCol_[i]", (double)BASE_VALUES[1]);
        check(1, "BooleanCol_[i]", true);
    }


    @Test
    public void testNoInput() {
        final String oldValue = Configuration.getInstance().getProperty("CompilerTools.logEnabledDefault");
        Configuration.getInstance().setProperty("CompilerTools.logEnabledDefault", "true");
        try {
            FormulaColumn formulaColumn = FormulaColumn.createFormulaColumn("Foo", "(String)\"1234\"");
            formulaColumn.initDef(Collections.emptyMap());
            final String result = (String) formulaColumn.getDataView().get(0);
            assertEquals(result, "1234");

            FormulaColumn longFormulaColumn = FormulaColumn.createFormulaColumn("Foo", "1234L");
            longFormulaColumn.initDef(Collections.emptyMap());
            final long longResult = longFormulaColumn.getDataView().getLong(0);
            assertEquals(longResult, 1234L);
        } finally {
            Configuration.getInstance().setProperty("CompilerTools.logEnabledDefault", oldValue);
        }
    }

    /**
     * More or less copied from TestDBLanguageParser
     */
    @Test
    public void testResolution() {
        for(int row=0;row<testDataTable.size();row++) {
            String expression = "Math.sqrt(5.0)";
            Object result = Math.sqrt(5.0);
            checkPrimitive(row, expression, result);

            expression = "Math.sqrt(5)";
            checkPrimitive(row, expression, result);

            expression = "log(5.0)";
            result = Math.log(5.0);
            checkPrimitive(row, expression, result);

            expression = "java.util.Collections.singletonList(5)";
            result = java.util.Collections.singletonList(5);
            checkPrimitive(row, expression, result);

            expression = "Arrays.asList(`varargTest1`, `varargTest2`, `varargTest3`)";
            result = Arrays.asList("varargTest1", "varargTest2", "varargTest3");
            checkPrimitive(row, expression, result);

            expression = "io.deephaven.db.tables.DefaultColumnDefinition.COLUMNTYPE_NORMAL";
            result = DefaultColumnDefinition.COLUMNTYPE_NORMAL;
            checkPrimitive(row, expression, result);

            expression = "CountDownLatch.class"; // (testing a package import)
            result = java.util.concurrent.CountDownLatch.class;
            checkPrimitive(row, expression, result);

            expression = "Calendar.JANUARY"; // (testing a class import)
            result = Calendar.JANUARY;
            checkPrimitive(row, expression, result);

            expression = "Calendar.getAvailableCalendarTypes()";
            result = Calendar.getAvailableCalendarTypes();
            checkPrimitive(row, expression, result);

            expression = "NULL_INT";
            result = io.deephaven.util.QueryConstants.NULL_INT;
            checkPrimitive(row, expression, result);

            expression = "Math.sqrt(DoubleCol)";
            result = Math.sqrt(DBLanguageFunctionUtil.doubleCast(BASE_VALUES[row]));
            checkPrimitive(row, expression, result);

            expression = "Math.sqrt(IntCol)";
            result = Math.sqrt(DBLanguageFunctionUtil.doubleCast(BASE_VALUES[row]));
            checkPrimitive(row, expression, result);
        }
    }

//    // TODO: Make this test pass.
//    public void testMethodNameUsedInFormulaClass1() {
//        /* A Formula's local "get() method will take precedence over one made available in a static import;
//           the parser should ensure that a method from a static import will not be masked by methods in
//           a generated Formula class.
//           (JLS 15.12.1; https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.12.1)
//         */
//        QueryLibrary.importStatic(TestFormulaColumnAuxiliaryClass1.class);
//        String expression = "get(27L)";
//        checkPrimitive(0, expression, 27L);
//    }
//
//    // TODO: Make this test pass.
//    public void testMethodNameUsedInFormulaClass2() {
//        /* A Formula's local "get() method will take precedence over one made available in a static import;
//           the parser should ensure that a method from a static import will not be masked by methods in
//           a generated Formula class.
//           (JLS 15.12.1; https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.12.1)
//         */
//        QueryLibrary.importStatic(TestFormulaColumnAuxiliaryClass2.class);
//        String expression = "get(27L)";
//        checkPrimitive(0, expression, 27L);
//    }

    @Test
    public void testObjectConstruction() {
        String expression;
        Object result;
        for(int row=0;row<testDataTable.size();row++) {
            expression = "new Integer(IntCol)";
            result = BASE_VALUES[row];
            check(row, expression, (int) result); // note that formula always return unboxed types

            expression = "Integer.toString(IntCol)";
            result = Integer.toString(BASE_VALUES[row]);
            check(row, expression, result);

            expression = "new String(`Hello, world!`)";
            result = "Hello, world!";
            check(row, expression, result);

            expression = "new String(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4)";
            result = new String(new char[]{'a', 'b', 'c', 'd', 'e'}, 1, 4);
            check(row, expression, result);

            expression = "new HashSet()";
            result = new HashSet();
            check(row, expression, result);

            expression = "new HashSet<String>()";
            result = new HashSet<String>();
            check(row, expression, result);

            expression = "new io.deephaven.db.tables.utils.DBDateTime(123L)";
            result = new io.deephaven.db.tables.utils.DBDateTime(123L);
            check(row, expression, result);
        }
    }

    @Test
    public void testException() {
        // Parse exceptions:
        checkExpectingParseException("this_variable_definitely_has_not_been_defined"); // non-existent variable

        // Runtime exceptions:
        checkExpectingEvaluationException("((Object) null).hashCode()"); // NPE
        checkExpectingEvaluationException("Long.parseLong(`IANAL`)"); // parse
        checkExpectingEvaluationException("myIntArray[99]"); // ArrayIndexOutOfBounds


        QueryScope.addParam("sdf", new SimpleDateFormat("MM/dd/yy"));

        FormulaColumn formulaColumn = FormulaColumn.createFormulaColumn("Foo", "sdf.format(sdf.parse(`11/21/16`))");
        formulaColumn.initDef(Collections.emptyMap());
        final String result = (String) formulaColumn.getDataView().get(0);
        assertEquals(result, "11/21/16");

        formulaColumn = FormulaColumn.createFormulaColumn("Foo", "sdf.format(sdf.parse(`11-21-16`))");
        formulaColumn.initDef(Collections.emptyMap());

        Exception caught = null;
        try {
            formulaColumn.getDataView().get(0);
        } catch (FormulaEvaluationException e) {
            caught = e;
        }
        assertNotNull(caught);
    }

    @Test
    public void testCasts() {
        double result;

        {
            FormulaColumn formulaColumn = FormulaColumn.createFormulaColumn("Foo", "(double)IntCol");
            formulaColumn.initDef(availableColumns);
            formulaColumn.initInputs(testDataTable);

            result = formulaColumn.getDataView().getDouble(0);
            assertEquals((double) BASE_VALUES[0], result);

            result = formulaColumn.getDataView().getDouble(1);
            assertEquals((double) BASE_VALUES[1], result);

            result = formulaColumn.getDataView().getDouble(2);
            assertEquals(io.deephaven.util.QueryConstants.NULL_DOUBLE, result);
        }

        {
            FormulaColumn formulaColumn = FormulaColumn.createFormulaColumn("Foo", "(double)myIntObj");
            formulaColumn.initDef(availableColumns);
            result = formulaColumn.getDataView().getDouble(0);
            assertEquals((double) QUERYSCOPE_OBJ_BASE_VALUE, result);
        }

        {   // Ensure there are no NPEs from unboxing while evaluating a formula.
            FormulaColumn formulaColumn = FormulaColumn.createFormulaColumn("Foo", "(Double)null");
            formulaColumn.initDef(availableColumns);
            result = formulaColumn.getDataView().getDouble(0);
            assertEquals(QueryConstants.NULL_DOUBLE, result);
        }
    }

    /**
     * Test casts among all primitive types.
     */
    @Test
    public void testPrimitiveCasts() {
        final List<Class> primitiveTypes = new ArrayList<>(io.deephaven.util.type.TypeUtils.PRIMITIVE_TYPES);

        for (int i = 0; i < io.deephaven.util.type.TypeUtils.PRIMITIVE_TYPES.size(); i++) {
            final Class sourceType = primitiveTypes.get(i);
            final String sourceTypeName = sourceType.getName();
            final String sourceColName = Character.toUpperCase(sourceTypeName.charAt(0)) + sourceTypeName.substring(1) + "Col";

            for (int j = 0; j < io.deephaven.util.type.TypeUtils.PRIMITIVE_TYPES.size(); j++) {
                final Class destType = primitiveTypes.get(j);
                final String destTypeName = destType.getName();

                String expression = "(" + destTypeName + ")" + sourceColName; // e.g. "TestCast=(int)myShortObj"
                final Object[] expectedResults = new Object[testDataTable.intSize("TestFormulaColumn.testPrimitiveCasts")];
                if (destType == boolean.class) {
                    expectedResults[0] = false;
                    expectedResults[1] = true;
                    expectedResults[2] = null;
                } else {
                    expectedResults[0] = getBoxedBaseVal(0, destType);
                    expectedResults[1] = getBoxedBaseVal(1, destType);
                    expectedResults[2] = getBoxedBaseVal(2, destType);
                }

                for (int row = 0; row < testDataTable.size(); row++) {
                    try {
                        if (  // booleans can only be cast to/from booleans; should be a compile time exception otherwise
                                sourceType == boolean.class ^ destType == boolean.class
                                        // also, we should hit a runtime exception casting null to boolean (NPE while unboxing)
                                        || (sourceType == boolean.class && row == NULL_ROW_INDEX)
                                ) {
                            checkExpectingException(row, expression);
                        } else {
                            checkPrimitive(row, expression, expectedResults[row]);
                        }
                    } catch (Throwable ex) {
                        throw new RuntimeException("Failed testing cast of " + sourceType.getName() + " to " + destType.getName() +
                                " (i=" + i + ", j=" + j + ", row=" + row + "). Formula:\n" + expression, ex);
                    }
                }
            }
        }
    }

    /**
     * Test unboxing conversions, including null pointers and boxed nulls
     */
    @Test
    public void testUnboxingCasts() {
        final List<Class> boxedTypes = new ArrayList<>(io.deephaven.util.type.TypeUtils.BOXED_TYPES);
        final List<Class> primitiveTypes = new ArrayList<>(io.deephaven.util.type.TypeUtils.PRIMITIVE_TYPES);

        for (int i = 0; i < boxedTypes.size(); i++) {
            final Class sourceType = boxedTypes.get(i);
            final String sourceTypeName = sourceType.getSimpleName();
            final String unboxedSourceTypeName = io.deephaven.util.type.TypeUtils.getUnboxedType(sourceType).getName();
            final String unboxedSourceTypeNameProperCase = Character.toUpperCase(unboxedSourceTypeName.charAt(0)) + unboxedSourceTypeName.substring(1);
            final String sourceColName = unboxedSourceTypeNameProperCase + "Col";
            final String boxedTypeConstructorCall = "new " + sourceTypeName + '(' + sourceColName + ')';

            for (int j = 0; j < primitiveTypes.size(); j++) {
                Class destType = primitiveTypes.get(j);
                final String destTypeName = destType.getName();
                final String destTypeCast = '(' + destTypeName + ')';
                final String expression = destTypeCast + boxedTypeConstructorCall; // e.g. "(double)new Integer(IntCol)"

                final Object[] expectedResults = new Object[3];
                final String boxedTypeNullPointer, boxedQueryConstantsNull;
                if (destType == boolean.class) {
                    expectedResults[0] = false;
                    expectedResults[1] = true;
                    expectedResults[2] = null;

                    boxedTypeNullPointer = null; // would be "(Boolean)null", but tests don't use this
                    boxedQueryConstantsNull = null; // does not apply to booleans in the db.
                } else { // i != j; Unboxing/unboxing and widening conversions

                    expectedResults[0] = getBoxedBaseVal(0, destType);
                    expectedResults[1] = getBoxedBaseVal(1, destType);
                    expectedResults[2] = getBoxedBaseVal(2, destType);

                    boxedTypeNullPointer = '(' + sourceTypeName + ")null"; // e.g. "(Byte)null"
                    boxedQueryConstantsNull = "new " + sourceTypeName + "(NULL_" + unboxedSourceTypeName.toUpperCase() + ')'; // e.g. new Byte(NULL_BYTE)
                }

                boolean compileTimeUnsupportedConversion =
                        // only unboxing or unboxing+widening is allowed; i > j means i wider than j
                        i > j
                                // Booleans, and only Booleans, can be unboxed to booleans. Should be a parse exception otherwise
                                || sourceType == Boolean.class ^ destType == boolean.class
                                // also, Byte/Short can't be cast to char
                                || destType == char.class && (sourceType == Byte.class || sourceType == Short.class);

                try {
                    for (int row = 0; row < BASE_VALUES.length; row++) {
                        try {
                            if (compileTimeUnsupportedConversion) {
                                checkExpectingParseException(row, expression);
                            } else if (sourceType == Boolean.class && row == NULL_ROW_INDEX) { // unboxing null reference causes runtime NPE
                                checkExpectingException(row, NullPointerException.class, expression);
                            } else {
                                checkPrimitive(row, expression, expectedResults[row]);
                            }
                        } catch (Throwable t) {
                            throw new RuntimeException("Failed at row=" + row, t);
                        }
                    }

                    if (!compileTimeUnsupportedConversion && !sourceType.equals(Boolean.class)) {
                        // Test unboxing a boxed QueryConstants null value. This should unbox, then convert.
                        // Thus we should have: (short)new Integer(NULL_INT) --> shortCast(intCast(new Integer(NULL_INT))) --> NULL_SHORT
                        final String conversionOfBoxedQCNull = destTypeCast + boxedQueryConstantsNull;
                        checkPrimitive(conversionOfBoxedQCNull, expectedResults[NULL_ROW_INDEX]);

                        // Test unboxing a null pointer of a boxed type. This should be converted into a
                        // QueryConstants null value of the appropriate type.
                        final String conversionOfNullBoxedType = destTypeCast + boxedTypeNullPointer;
                        checkPrimitive(conversionOfNullBoxedType, expectedResults[NULL_ROW_INDEX]);
                    }


                } catch (Throwable ex) {
                    throw new RuntimeException("Failed testing cast of " + sourceType.getName() + " to " + destType.getName() +
                            " (i=" + i + ", j=" + j + "). Formula:\n" + expression, ex);
                }
            }
        }
    }

    @Test
    public void testWrapWithCastIfNecessary() {
        testWrapWithCastHelper(Byte.class, "byteCast");
        testWrapWithCastHelper(Character.class, "charCast");
        testWrapWithCastHelper(Short.class, "shortCast");
        testWrapWithCastHelper(Integer.class, "intCast");
        testWrapWithCastHelper(Long.class, "longCast");
        testWrapWithCastHelper(Float.class, "floatCast");
        testWrapWithCastHelper(Double.class, "doubleCast");
        testWrapWithCastHelper(Boolean.class, null);
        testWrapWithCastHelper(String.class, null);
        testWrapWithCastHelper(Object.class, null);
    }

    private void testWrapWithCastHelper(final Class type, final String cast) {
        final String theFormula = "theFormula";
        final String expected = cast == null ? theFormula :
                DBLanguageFunctionUtil.class.getCanonicalName() + '.' + cast + '(' + theFormula + ')';
        final TypeAnalyzer ta = TypeAnalyzer.create(type);
        final String possiblyWrappedExpression = ta.wrapWithCastIfNecessary(theFormula);
        Assert.equals(possiblyWrappedExpression, "possiblyWrappedExpression", expected);
    }

    //---------- METHODS TO ASSIST WITH TESTING

    private FormulaColumn initCheck(String formulaString) {
        FormulaColumn formulaColumn = FormulaColumn.createFormulaColumn("Foo", formulaString);
        formulaColumn.initDef(availableColumns);
        formulaColumn.initInputs(testDataTable);
        return formulaColumn;
    }

    ////---- These are just different versions of check():

    private void check(int index, String formulaString, Object expectedResult) {
        FormulaColumn formulaColumn = initCheck(formulaString);
        Object result = formulaColumn.getDataView().get(index);
        assertEquals(expectedResult, result);
    }

    private void check(int index, String formulaString, byte expectedResult) {
        FormulaColumn formulaColumn = initCheck(formulaString);
        byte result = formulaColumn.getDataView().getByte(index);
        assertEquals(expectedResult, result);
    }

    private void check(int index, String formulaString, short expectedResult) {
        FormulaColumn formulaColumn = initCheck(formulaString);
        short result = formulaColumn.getDataView().getShort(index);
        assertEquals(expectedResult, result);
    }

    private void check(int index, String formulaString, char expectedResult) {
        FormulaColumn formulaColumn = initCheck(formulaString);
        char result = formulaColumn.getDataView().getChar(index);
        assertEquals(expectedResult, result);
    }

    private void check(int index, String formulaString, int expectedResult) {
        FormulaColumn formulaColumn = initCheck(formulaString);
        int result = formulaColumn.getDataView().getInt(index);
        assertEquals(expectedResult, result);
    }

    private void check(int index, String formulaString, long expectedResult) {
        FormulaColumn formulaColumn = initCheck(formulaString);
        long result = formulaColumn.getDataView().getLong(index);
        assertEquals(expectedResult, result);
    }

    private void check(int index, String formulaString, float expectedResult) {
        FormulaColumn formulaColumn = initCheck(formulaString);
        float result = formulaColumn.getDataView().getFloat(index);
        assertEquals(expectedResult, result);
    }

    private void check(int index, String formulaString, double expectedResult) {
        FormulaColumn formulaColumn = initCheck(formulaString);
        double result = formulaColumn.getDataView().getDouble(index);
        assertEquals(expectedResult, result);
    }

    private void check(String formulaString, Object expectedResult) {
        check(0, formulaString, expectedResult);
    }

    private void check(String formulaString, byte expectedResult) {
        check(0, formulaString, expectedResult);
    }

    private void check(String formulaString, short expectedResult) {
        check(0, formulaString, expectedResult);
    }

    private void check(String formulaString, char expectedResult) {
        check(0, formulaString, expectedResult);
    }

    private void check(String formulaString, int expectedResult) {
        check(0, formulaString, expectedResult);
    }

    private void check(String formulaString, long expectedResult) {
        check(0, formulaString, expectedResult);
    }

    private void check(String formulaString, float expectedResult) {
        check(0, formulaString, expectedResult);
    }

    private void check(String formulaString, double expectedResult) {
        check(0, formulaString, expectedResult);
    }

    private void checkPrimitive(String formulaString, Object expectedResult) {
        checkPrimitive(0, formulaString, expectedResult);
    }

    /**
     * Invokes the appropriate {@code check()} function for the boxed expected result
     *
     * @param formulaString  The formula to check
     * @param expectedResult The expected result. Must be a boxed type (e.g. {@link Integer}, {@link Character}, etc.
     * @param index          The index to check
     */
    private void checkPrimitive(int index, String formulaString, Object expectedResult) {
        Class unboxedType = expectedResult == null ? null : TypeUtils.getUnboxedType(expectedResult.getClass());

        if (unboxedType == byte.class) {
            check(index, formulaString, ((Number) expectedResult).byteValue());
        } else if (unboxedType == short.class) {
            check(index, formulaString, ((Number) expectedResult).shortValue());
        } else if (unboxedType == char.class) {
            check(index, formulaString, ((Character) expectedResult).charValue());
        } else if (unboxedType == int.class) {
            check(index, formulaString, ((Number) expectedResult).intValue());
        } else if (unboxedType == long.class) {
            check(index, formulaString, ((Number) expectedResult).longValue());
        } else if (unboxedType == float.class) {
            check(index, formulaString, ((Number) expectedResult).floatValue());
        } else if (unboxedType == double.class) {
            check(index, formulaString, ((Number) expectedResult).doubleValue());
        } else {
            check(index, formulaString, expectedResult);
        }
    }

    private void checkExpectingException(String formulaString) {
        checkExpectingException(0, formulaString);
    }

    private void checkExpectingException(int index, String formulaString) {
        checkExpectingException(index, null, formulaString);
    }

    /**
     * @param index                 The index of {@link #testDataTable} at which {@code formulaString} should be evaluateds
     * @param exceptionTypeToExpect Expect an exception of this type, and not of any other (i.e. runtime or compilation) exception.
     *                              If null, any {@code Exception} is expected.
     * @param formulaString         The formula to evaluate
     */
    private void checkExpectingException(int index, Class<? extends Exception> exceptionTypeToExpect, String formulaString) {

        final boolean expectSpecificInspection = exceptionTypeToExpect != null;

        boolean anyExceptionWasThrown = false;
        boolean expectedExceptionWasThrown = false;

        Exception theException = null;

        try {
            check(index, formulaString, null);
        } catch (Exception ex) {
            theException = ex;

            if (expectSpecificInspection) {
                expectedExceptionWasThrown = involvesExceptionType(ex, exceptionTypeToExpect);
            }
            anyExceptionWasThrown = !expectSpecificInspection || !expectedExceptionWasThrown;
        }

        if (expectSpecificInspection) {
            if (!expectedExceptionWasThrown) {
                throw new AssertionFailure("Expected exception " + exceptionTypeToExpect.getName() + " was not thrown; another exception was", theException);
            }
        } else if (!anyExceptionWasThrown) {
            fail("Should have thrown an exception");
        }
    }

    private void checkExpectingParseException(String formulaString) {
        checkExpectingParseException(0, formulaString);
    }

    private void checkExpectingParseException(int index, String formulaString) {
        checkExpectingException(index, QueryLanguageParseException.class, formulaString);
    }

    private void checkExpectingEvaluationException(String formulaString) {
        checkExpectingEvaluationException(0, formulaString);
    }

    private void checkExpectingEvaluationException(int index, String formulaString) {
        checkExpectingException(index, FormulaEvaluationException.class, formulaString);
    }

    /**
     * Returns true if {@code t} is an exception of {@code exceptionType} or was caused by one.
     *
     * @param t             The throwable to check. Cannot be null.
     * @param exceptionType The type to check against
     */
    private static boolean involvesExceptionType(Throwable t, Class<? extends Exception> exceptionType) {
        Assert.neqNull(t, "t");
        for (; t != null; t = t.getCause()) {
            if (exceptionType.isAssignableFrom(t.getClass())) {
                return true;
            }
        }
        return false;
    }

    private enum TestFormulaColumnEnum {
        ONE, TWO
    }

    @SuppressWarnings("WeakerAccess")
    public static class TestFormulaColumnAuxiliaryClass1 {

        // 2017-10-07: Statically importing and calling this method will lead to a Formula class body we cannot compile
        private static long get(long arg) {
            return arg;
        }

    }

    @SuppressWarnings("WeakerAccess")
    public static class TestFormulaColumnAuxiliaryClass2 {

        // 2017-10-07: Statically importing and calling this method leads to a StackOverflowError
        private static Object get(long arg) {
            return arg;
        }

    }

}
