/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.legacy.rewriter.nestedfield;

import static com.alibaba.druid.sql.ast.statement.SQLJoinTableSource.JoinType;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Nested field information in current query being visited. */
class Scope {

  /** Join Type as passed in the actual SQL subquery */
  private JoinType actualJoinType;

  /** Alias of parent such as alias "t" of parent table "team" in "FROM team t, t.employees e" */
  private String parentAlias;

  /**
   * Mapping from nested field path alias to path full name in FROM. eg. e in {e => employees} in
   * "FROM t.employees e"
   */
  private final Map<String, String> aliasFullPaths = new HashMap<>();

  /**
   * Mapping from binary operation condition (in WHERE) to nested field tag (full path for nested,
   * EMPTY for non-nested field)
   */
  private final Map<SQLBinaryOpExpr, String> conditionTags = new IdentityHashMap<>();

  String getParentAlias() {
    return parentAlias;
  }

  void setParentAlias(String parentAlias) {
    this.parentAlias = parentAlias;
  }

  void addAliasFullPath(String alias, String path) {
    if (alias.isEmpty()) {
      aliasFullPaths.put(path, path);
    } else {
      aliasFullPaths.put(alias, path);
    }
  }

  String getFullPath(String alias) {
    return aliasFullPaths.getOrDefault(alias, "");
  }

  boolean isAnyNestedField() {
    return !aliasFullPaths.isEmpty();
  }

  Set<String> getAliases() {
    return aliasFullPaths.keySet();
  }

  String getConditionTag(SQLBinaryOpExpr expr) {
    return conditionTags.getOrDefault(expr, "");
  }

  void addConditionTag(SQLBinaryOpExpr expr, String tag) {
    conditionTags.put(expr, tag);
  }

  JoinType getActualJoinType() {
    return actualJoinType;
  }

  void setActualJoinType(JoinType joinType) {
    actualJoinType = joinType;
  }
}
