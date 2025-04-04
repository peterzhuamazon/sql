/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.calcite.utils;

import static java.lang.Math.E;
import static org.opensearch.sql.calcite.utils.OpenSearchTypeFactory.getLegacyTypeName;
import static org.opensearch.sql.calcite.utils.UserDefinedFunctionUtils.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.fun.SqlLibraryOperators;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.fun.SqlTrimFunction;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlTypeName;
import org.opensearch.sql.calcite.CalcitePlanContext;
import org.opensearch.sql.calcite.udf.SpanFunction;
import org.opensearch.sql.calcite.udf.conditionUDF.IfFunction;
import org.opensearch.sql.calcite.udf.conditionUDF.IfNullFunction;
import org.opensearch.sql.calcite.udf.conditionUDF.NullIfFunction;
import org.opensearch.sql.calcite.udf.mathUDF.CRC32Function;
import org.opensearch.sql.calcite.udf.mathUDF.ConvFunction;
import org.opensearch.sql.calcite.udf.mathUDF.EulerFunction;
import org.opensearch.sql.calcite.udf.mathUDF.ModFunction;
import org.opensearch.sql.calcite.udf.mathUDF.SqrtFunction;
import org.opensearch.sql.calcite.udf.systemUDF.TypeOfFunction;
import org.opensearch.sql.calcite.udf.textUDF.LocateFunction;
import org.opensearch.sql.calcite.udf.textUDF.ReplaceFunction;

public interface BuiltinFunctionUtils {

  static SqlOperator translate(String op) {
    switch (op.toUpperCase(Locale.ROOT)) {
      case "AND":
        return SqlStdOperatorTable.AND;
      case "OR":
        return SqlStdOperatorTable.OR;
      case "NOT":
        return SqlStdOperatorTable.NOT;
      case "XOR":
      case "!=":
        return SqlStdOperatorTable.NOT_EQUALS;
      case "=":
        return SqlStdOperatorTable.EQUALS;
      case "<>":
      case ">":
        return SqlStdOperatorTable.GREATER_THAN;
      case ">=":
        return SqlStdOperatorTable.GREATER_THAN_OR_EQUAL;
      case "<":
        return SqlStdOperatorTable.LESS_THAN;
      case "<=":
        return SqlStdOperatorTable.LESS_THAN_OR_EQUAL;
      case "REGEXP":
        return SqlLibraryOperators.REGEXP;
      case "+":
        return SqlStdOperatorTable.PLUS;
      case "-":
        return SqlStdOperatorTable.MINUS;
      case "*":
        return SqlStdOperatorTable.MULTIPLY;
      case "/":
        return SqlStdOperatorTable.DIVIDE;
        // Built-in String Functions
      case "ASCII":
        return SqlStdOperatorTable.ASCII;
      case "CONCAT":
        return SqlLibraryOperators.CONCAT_FUNCTION;
      case "CONCAT_WS":
        return SqlLibraryOperators.CONCAT_WS;
      case "LIKE":
        return SqlLibraryOperators.ILIKE;
      case "LTRIM", "RTRIM", "TRIM":
        return SqlStdOperatorTable.TRIM;
      case "LENGTH":
        return SqlStdOperatorTable.CHAR_LENGTH;
      case "LOWER":
        return SqlStdOperatorTable.LOWER;
      case "POSITION":
        return SqlStdOperatorTable.POSITION;
      case "REVERSE":
        return SqlLibraryOperators.REVERSE;
      case "RIGHT":
        return SqlLibraryOperators.RIGHT;
      case "LEFT":
        return SqlLibraryOperators.LEFT;
      case "SUBSTRING", "SUBSTR":
        return SqlStdOperatorTable.SUBSTRING;
      case "STRCMP":
        return SqlLibraryOperators.STRCMP;
      case "REPLACE":
        return TransferUserDefinedFunction(ReplaceFunction.class, "REPLACE", ReturnTypes.CHAR);
      case "LOCATE":
        return TransferUserDefinedFunction(
            LocateFunction.class,
            "LOCATE",
            getNullableReturnTypeInferenceForFixedType(SqlTypeName.INTEGER));
      case "UPPER":
        return SqlStdOperatorTable.UPPER;
        // Built-in Math Functions
      case "ABS":
        return SqlStdOperatorTable.ABS;
      case "ACOS":
        return SqlStdOperatorTable.ACOS;
      case "ASIN":
        return SqlStdOperatorTable.ASIN;
      case "ATAN", "ATAN2":
        return SqlStdOperatorTable.ATAN2;
      case "CEILING":
        return SqlStdOperatorTable.CEIL;
      case "CONV":
        // The CONV function in PPL converts between numerical bases,
        // while SqlStdOperatorTable.CONVERT converts between charsets.
        return TransferUserDefinedFunction(ConvFunction.class, "CONVERT", ReturnTypes.VARCHAR);
      case "COS":
        return SqlStdOperatorTable.COS;
      case "COT":
        return SqlStdOperatorTable.COT;
      case "CRC32":
        return TransferUserDefinedFunction(CRC32Function.class, "CRC32", ReturnTypes.BIGINT);
      case "DEGREES":
        return SqlStdOperatorTable.DEGREES;
      case "E":
        return TransferUserDefinedFunction(EulerFunction.class, "E", ReturnTypes.DOUBLE);
      case "EXP":
        return SqlStdOperatorTable.EXP;
      case "FLOOR":
        return SqlStdOperatorTable.FLOOR;
      case "LN":
        return SqlStdOperatorTable.LN;
      case "LOG":
        return SqlLibraryOperators.LOG;
      case "LOG2":
        return SqlLibraryOperators.LOG2;
      case "LOG10":
        return SqlStdOperatorTable.LOG10;
      case "MOD", "%":
        // The MOD function in PPL supports floating-point parameters, e.g., MOD(5.5, 2) = 1.5,
        // MOD(3.1, 2.1) = 1.1,
        // whereas SqlStdOperatorTable.MOD supports only integer / long parameters.
        return TransferUserDefinedFunction(
            ModFunction.class,
            "MOD",
            getLeastRestrictiveReturnTypeAmongArgsAt(List.of(0, 1), true));
      case "PI":
        return SqlStdOperatorTable.PI;
      case "POW", "POWER":
        return SqlStdOperatorTable.POWER;
      case "RADIANS":
        return SqlStdOperatorTable.RADIANS;
      case "RAND":
        return SqlStdOperatorTable.RAND;
      case "ROUND":
        return SqlStdOperatorTable.ROUND;
      case "SIGN":
        return SqlStdOperatorTable.SIGN;
      case "SIN":
        return SqlStdOperatorTable.SIN;
      case "SQRT":
        // SqlStdOperatorTable.SQRT is declared but not implemented, therefore we use a custom
        // implementation.
        return TransferUserDefinedFunction(
            SqrtFunction.class, "SQRT", ReturnTypes.DOUBLE_FORCE_NULLABLE);
      case "CBRT":
        return SqlStdOperatorTable.CBRT;
        // Built-in Date Functions
      case "CURRENT_TIMESTAMP":
        return SqlStdOperatorTable.CURRENT_TIMESTAMP;
      case "CURRENT_DATE":
        return SqlStdOperatorTable.CURRENT_DATE;
      case "DATE":
        return SqlLibraryOperators.DATE;
      case "ADDDATE":
        return SqlLibraryOperators.DATE_ADD_SPARK;
      case "DATE_ADD":
        return SqlLibraryOperators.DATEADD;
        // UDF Functions
      case "SPAN":
        return TransferUserDefinedFunction(
            SpanFunction.class, "SPAN", ReturnTypes.ARG0_FORCE_NULLABLE);
        // Built-in condition functions
      case "IF":
        return TransferUserDefinedFunction(IfFunction.class, "if", getReturnTypeInference(1));
      case "IFNULL":
        return TransferUserDefinedFunction(
            IfNullFunction.class, "ifnull", getReturnTypeInference(1));
      case "NULLIF":
        return TransferUserDefinedFunction(
            NullIfFunction.class, "ifnull", getReturnTypeInference(0));
      case "IS NOT NULL":
        return SqlStdOperatorTable.IS_NOT_NULL;
      case "IS NULL":
        return SqlStdOperatorTable.IS_NULL;
      case "TYPEOF":
        // TODO optimize this function to ImplementableFunction
        return TransferUserDefinedFunction(
            TypeOfFunction.class, "typeof", ReturnTypes.VARCHAR_2000_NULLABLE);
        // TODO Add more, ref RexImpTable
      default:
        throw new IllegalArgumentException("Unsupported operator: " + op);
    }
  }

  /**
   * Translates function arguments to align with Calcite's expectations, ensuring compatibility with
   * PPL (Piped Processing Language). This is necessary because Calcite's input argument order or
   * default values may differ from PPL's function definitions.
   *
   * @param op The function name as a string.
   * @param argList A list of {@link RexNode} representing the parsed arguments from the PPL
   *     statement.
   * @param context The {@link CalcitePlanContext} providing necessary utilities such as {@code
   *     rexBuilder}.
   * @return A modified list of {@link RexNode} that correctly maps to Calcite’s function
   *     expectations.
   */
  static List<RexNode> translateArgument(
      String op, List<RexNode> argList, CalcitePlanContext context) {
    switch (op.toUpperCase(Locale.ROOT)) {
      case "TRIM":
        List<RexNode> trimArgs =
            new ArrayList<>(
                List.of(
                    context.rexBuilder.makeFlag(SqlTrimFunction.Flag.BOTH),
                    context.rexBuilder.makeLiteral(" ")));
        trimArgs.addAll(argList);
        return trimArgs;
      case "LTRIM":
        List<RexNode> LTrimArgs =
            new ArrayList<>(
                List.of(
                    context.rexBuilder.makeFlag(SqlTrimFunction.Flag.LEADING),
                    context.rexBuilder.makeLiteral(" ")));
        LTrimArgs.addAll(argList);
        return LTrimArgs;
      case "RTRIM":
        List<RexNode> RTrimArgs =
            new ArrayList<>(
                List.of(
                    context.rexBuilder.makeFlag(SqlTrimFunction.Flag.TRAILING),
                    context.rexBuilder.makeLiteral(" ")));
        RTrimArgs.addAll(argList);
        return RTrimArgs;
      case "STRCMP":
        List<RexNode> StrcmpArgs = List.of(argList.get(1), argList.get(0));
        return StrcmpArgs;
      case "ATAN":
        List<RexNode> AtanArgs = new ArrayList<>(argList);
        if (AtanArgs.size() == 1) {
          BigDecimal divideNumber = BigDecimal.valueOf(1);
          AtanArgs.add(context.rexBuilder.makeBigintLiteral(divideNumber));
        }
        return AtanArgs;
      case "LOG":
        List<RexNode> LogArgs = new ArrayList<>();
        RelDataTypeFactory typeFactory = context.rexBuilder.getTypeFactory();
        if (argList.size() == 1) {
          LogArgs.add(argList.getFirst());
          LogArgs.add(
              context.rexBuilder.makeExactLiteral(
                  BigDecimal.valueOf(E), typeFactory.createSqlType(SqlTypeName.DOUBLE)));
        } else if (argList.size() == 2) {
          LogArgs.add(argList.get(1));
          LogArgs.add(argList.get(0));
        } else {
          throw new IllegalArgumentException("Log cannot accept argument list: " + argList);
        }
        return LogArgs;
      case "TYPEOF":
        return List.of(
            context.rexBuilder.makeLiteral(
                getLegacyTypeName(argList.getFirst().getType(), context.queryType)));
      default:
        return argList;
    }
  }
}
