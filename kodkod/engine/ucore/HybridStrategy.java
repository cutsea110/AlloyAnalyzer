/* 
 * Kodkod -- Copyright (c) 2005-present, Emina Torlak
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package kodkod.engine.ucore;

import java.util.Iterator;

import kodkod.engine.fol2sat.TranslationLog;
import kodkod.engine.fol2sat.Translator;
import kodkod.engine.satlab.Clause;
import kodkod.engine.satlab.ReductionStrategy;
import kodkod.engine.satlab.ResolutionTrace;
import kodkod.util.ints.IntBitSet;
import kodkod.util.ints.IntIterator;
import kodkod.util.ints.IntSet;
import kodkod.util.ints.Ints;

/**
 * A hybrid strategy for generating unsat cores that are minimal when mapped
 * back onto the logic level.  Specifically, let C be a core that is minimal
 * according to this strategy, and let F(C) be the top-level logic constraints
 * corresponding to C.  Then, this strategy guarantees that there is no clause
 * c in C such that F(C - c) is a strict subset of F(C). Note that this does
 * not guarantee that F(C) itself is minimal.  In other words, there could be 
 * an f in F(C) such that F(C) - f is unsatisfiable.  To get a minimal logic core,
 * use {@linkplain RCEStrategy}. 
 * 
 * <p>This strategy will work properly only on CNFs generated by the kodkod {@linkplain Translator}. </p>
 * @author Emina Torlak
 * @see RCEStrategy
 */
public final class HybridStrategy implements ReductionStrategy {
	private final IntSet topVars;
	
	/**
	 * Constructs a hybrid strategy that will use the given translation
	 * log to relate the cnf clauses back to the logic constraints from 
	 * which they were generated.
	 */
	public HybridStrategy(TranslationLog log) {
		topVars = StrategyUtils.rootVars(log);
	}
		
	/**
	 * {@inheritDoc}
	 * @see kodkod.engine.satlab.ReductionStrategy#next(kodkod.engine.satlab.ResolutionTrace)
	 */
	public IntSet next(ResolutionTrace trace) {
		if (topVars.isEmpty()) return Ints.EMPTY_SET; // tried everything
		final IntSet core = trace.core();
		
		for(Iterator<Clause> iter = trace.iterator(core); iter.hasNext();) {
			Clause clause = iter.next();
			int maxVar = clause.maxVariable();
			if (topVars.remove(maxVar)) {
				// get all core clauses with the given maximum variable
				IntSet exclude = coreClausesWithMaxVar(trace, maxVar);
				assert !exclude.isEmpty();
				//	get all clauses reachable from the conflict clause
				IntSet next = trace.reachable(Ints.singleton(trace.size()-1)); 
				// remove all clauses backward reachable from the clauses with the given maxVar
				next.removeAll(trace.backwardReachable(exclude));
				if (!next.isEmpty()) {
					return next;
				}
			}
		}
		
		topVars.clear();
		return Ints.EMPTY_SET;
	}
	
	/**
	 * Returns the indices of the clauses in the unsatisfiable core of the
	 * given trace that have the specified maximum variable.
	 * @return { i: trace.core() | trace[i].maxVariable() = maxVariable }
	 */
	private static IntSet coreClausesWithMaxVar(ResolutionTrace trace, int maxVariable) {
		final IntSet core = trace.core();
		final IntSet restricted = new IntBitSet(core.max()+1);
		final Iterator<Clause> clauses = trace.iterator(core);
		final IntIterator indices = core.iterator();
		while(clauses.hasNext()) {
			Clause clause = clauses.next();
			int index = indices.next();
			if (clause.maxVariable()==maxVariable)
				restricted.add(index);
		}
		return restricted;
	}
	
}
