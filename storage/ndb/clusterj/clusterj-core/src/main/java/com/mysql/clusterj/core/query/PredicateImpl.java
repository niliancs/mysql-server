/*
   Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; version 2 of the License.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301  USA
*/

package com.mysql.clusterj.core.query;


import com.mysql.clusterj.ClusterJException;
import com.mysql.clusterj.ClusterJFatalInternalException;
import com.mysql.clusterj.ClusterJUserException;

import com.mysql.clusterj.core.spi.QueryExecutionContext;
import com.mysql.clusterj.core.store.IndexScanOperation;
import com.mysql.clusterj.core.store.Operation;
import com.mysql.clusterj.core.store.ScanFilter;
import com.mysql.clusterj.core.store.ScanOperation;

import com.mysql.clusterj.core.util.I18NHelper;
import com.mysql.clusterj.core.util.Logger;
import com.mysql.clusterj.core.util.LoggerFactoryService;

import com.mysql.clusterj.query.Predicate;

import java.util.Comparator;
import java.util.TreeSet;

public abstract class PredicateImpl implements Predicate {

    /** My message translator */
    static final I18NHelper local = I18NHelper.getInstance(PredicateImpl.class);

    /** My logger */
    static final Logger logger = LoggerFactoryService.getFactory().getInstance(PredicateImpl.class);

    /** My domain object. */
    protected QueryDomainTypeImpl<?> dobj;

    /** The primary/unique index for this query if it exists */
    CandidateIndexImpl uniqueIndex;

    /** The comparator for candidate indices, ordered descending by score */
    Comparator<CandidateIndexImpl> candidateIndexComparator = new Comparator<CandidateIndexImpl>() {
        public int compare(CandidateIndexImpl o1, CandidateIndexImpl o2) {
            return o2.score - o1.score;
        }
    };

    /** The candidate indices ordered by score */
    private TreeSet<CandidateIndexImpl> scoredCandidateIndices =
        new TreeSet<CandidateIndexImpl>(candidateIndexComparator);

    /** Scan types. */
    protected enum ScanType {
        INDEX_SCAN,
        TABLE_SCAN,
        UNIQUE_KEY,
        PRIMARY_KEY
    }

    /** Indicates no bound set while setting bounds on index operations */
    public static int NO_BOUND_SET = 0;
    /** Indicates lower bound set while setting bounds on index operations */
    public static int LOWER_BOUND_SET = 1;
    /** Indicates upper bound set while setting bounds on index operations */
    public static int UPPER_BOUND_SET = 2;
    /** Indicates both bounds set while setting bounds on index operations */
    public static int BOTH_BOUNDS_SET = 3;

    public PredicateImpl(QueryDomainTypeImpl<?> dobj) {
        this.dobj = dobj;
    }

    public Predicate or(Predicate other) {
        assertPredicateImpl(other);
        PredicateImpl otherPredicateImpl = (PredicateImpl)other;
        assertIdenticalDomainObject(otherPredicateImpl, "or");
        return new OrPredicateImpl(this.dobj, this, otherPredicateImpl);
    }

    public Predicate and(Predicate other) {
        assertPredicateImpl(other);
        PredicateImpl predicateImpl = (PredicateImpl)other;
        assertIdenticalDomainObject(predicateImpl, "and");
        if (other instanceof AndPredicateImpl) {
            AndPredicateImpl andPredicateImpl = (AndPredicateImpl)other;
            return andPredicateImpl.and(this);
        } else {
            return new AndPredicateImpl(dobj, this, predicateImpl);
        }
    }

    public Predicate not() {
        return new NotPredicateImpl(this);
    }

    void markBoundsForCandidateIndices(QueryExecutionContext context, CandidateIndexImpl[] candidateIndices) {
        // default is nothing to do
    }

    public int operationSetBounds(QueryExecutionContext context,
            IndexScanOperation op, boolean lastColumn) {
        throw new ClusterJFatalInternalException(
                local.message("ERR_Implementation_Should_Not_Occur"));
    }

    public int operationSetLowerBound(QueryExecutionContext context,
            IndexScanOperation op, boolean lastColumn) {
        throw new ClusterJFatalInternalException(
                local.message("ERR_Implementation_Should_Not_Occur"));
    }

    public int operationSetUpperBound(QueryExecutionContext context,
            IndexScanOperation op, boolean lastColumn){
        throw new ClusterJFatalInternalException(
                local.message("ERR_Implementation_Should_Not_Occur"));
    }

    public void operationEqual(QueryExecutionContext context,
            Operation op) {
        throw new ClusterJFatalInternalException(
                local.message("ERR_Implementation_Should_Not_Occur"));
    }

    public void operationEqualFor(QueryExecutionContext context,
            Operation op, String indexName) {
        throw new ClusterJFatalInternalException(
                local.message("ERR_Implementation_Should_Not_Occur"));
    }

    public void objectSetValuesFor(QueryExecutionContext context,
            Object row, String indexName) {
        throw new ClusterJFatalInternalException(
                local.message("ERR_Implementation_Should_Not_Occur"));
    }

    /** Create a filter for the operation. Set the condition into the
     * new filter.
     * @param context the query execution context with the parameter values
     * @param op the operation
     */
    public void filterCmpValue(QueryExecutionContext context,
            ScanOperation op) {
        try {
            ScanFilter filter = op.getScanFilter(context);
            filter.begin();
            filterCmpValue(context, op, filter);
            filter.end();
        } catch (ClusterJException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ClusterJException(
                    local.message("ERR_Get_NdbFilter"), ex);
        }
    }

    public void filterCmpValue(QueryExecutionContext context,
            ScanOperation op, ScanFilter filter) {
        throw new ClusterJFatalInternalException(
                local.message("ERR_Implementation_Should_Not_Occur"));
    }

    public void assertIdenticalDomainObject(PredicateImpl other, String venue) {
        QueryDomainTypeImpl<?> otherDomainObject = other.getDomainObject();
        if (dobj != otherDomainObject) {
            throw new ClusterJUserException(
                    local.message("ERR_Wrong_Domain_Object", venue));
        }
    }

    /** Mark this predicate as being satisfied. */
    void setSatisfied() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /** Mark all parameters as being required. */
    public void markParameters() {
        // default is nothing to do
    }

    /** Unmark all parameters as being required. */
    public  void unmarkParameters() {
        // default is nothing to do
    }

    private void assertPredicateImpl(Predicate other) {
        if (!(other instanceof PredicateImpl)) {
            throw new UnsupportedOperationException(
                    local.message("ERR_NotImplemented"));
        }
    }

    private QueryDomainTypeImpl<?> getDomainObject() {
        return dobj;
    }

    public CandidateIndexImpl getBestCandidateIndex(QueryExecutionContext context) {
        return getBestCandidateIndexFor(context, getTopLevelPredicates());
    }

    /** Get the best candidate index for the query, considering all indices
     * defined and all predicates in the query. If a unique index is usable
     * (no non-null parameters) then return it. Otherwise, simply choose the
     * first index for which there is at least one leading non-null parameter.
     * @param predicates the predicates
     * @return the best index for the query
     */
    protected CandidateIndexImpl getBestCandidateIndexFor(QueryExecutionContext context,
            PredicateImpl... predicates) {
        // if there is a primary/unique index, see if it can be used in the current context
        if (uniqueIndex != null && uniqueIndex.isUsable(context)) {
            if (logger.isDebugEnabled()) logger.debug("usable unique index: " + uniqueIndex.getIndexName());
            return uniqueIndex;
        }
        // find the best candidate index by returning the highest scoring index that is usable
        // in the current context; i.e. has non-null parameters
        // TODO: it might be better to score indexes again considering the current context
        for (CandidateIndexImpl index: scoredCandidateIndices) {
            if (index.isUsable(context)) {
            if (logger.isDebugEnabled()) logger.debug("usable ordered index: " + index.getIndexName());
                return index;
            }
        }
        // there is no index that is usable in the current context
        return CandidateIndexImpl.getIndexForNullWhereClause();

    }

    /** Get the number of conditions in the top level predicate.
     * This is used to determine whether a hash index can be used. If there
     * are exactly the number of conditions as index columns, then the
     * hash index might be used.
     * By default (for equal, greaterThan, lessThan, greaterEqual, lessEqual)
     * there is one condition.
     * AndPredicateImpl overrides this method.
     * @return the number of conditions
     */
    protected int getNumberOfConditionsInPredicate() {
        return 1;
    }

    /** Analyze this predicate to determine whether a primary key, unique key, or ordered index
     * might be used. The result will be used during query execution once the actual parameters
     * are known.
     */
    public void prepare() {
        // Create CandidateIndexImpls
        CandidateIndexImpl[] candidateIndices = dobj.createCandidateIndexes();
        // Iterate over predicates and have each one register with
        // candidate indexes.
        for (PredicateImpl predicateImpl : getTopLevelPredicates()) {
            predicateImpl.markBoundsForCandidateIndices(candidateIndices);
        }
        // Iterate over candidate indices to find those that are usable.
        // Hash index operations require the predicates to have no extra conditions
        // beyond the index columns.
        // Btree index operations are ranked by the number of usable conditions
        int numberOfConditions = getNumberOfConditionsInPredicate();
        for (CandidateIndexImpl candidateIndex : candidateIndices) {
            if (candidateIndex.supportsConditionsOfLength(numberOfConditions)) {
                candidateIndex.score();
                int score = candidateIndex.getScore();
                if (score != 0) {
                    if (candidateIndex.isUnique()) {
                        // there can be only one unique index for a given predicate
                        uniqueIndex = candidateIndex;
                    } else {
                        // add possible indices to ordered map
                        scoredCandidateIndices.add(candidateIndex);
                    }
                }
                if (logger.isDetailEnabled()) {
                    logger.detail("Score: " + score + " from " + candidateIndex.getIndexName());
                }
            }
        }
    }

    protected void markBoundsForCandidateIndices(CandidateIndexImpl[] candidateIndices) {
        // default is nothing to do
    }

    /** Return an array of top level predicates that might be used with indices.
     * 
     * @return an array of top level predicates (defaults to {this}).
     */
    protected PredicateImpl[] getTopLevelPredicates() {
        return new PredicateImpl[] {this};
    }

    public ParameterImpl getParameter() {
        // default is there is no parameter for this predicate
        return null;
    }

    public boolean isUsable(QueryExecutionContext context) {
        return false;
    }

}
